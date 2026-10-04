package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.*;
import com.community.waste.mq.DomainEvent;
import com.community.waste.mq.EventPublisher;
import com.community.waste.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 清运公司派单：综合门禁/限行、车辆容量、垃圾点容量、已有派单冲突安排车次与路线。
 */
@Service
public class DispatchService {

    private static final EnumSet<TripStatus> ACTIVE_TRIP =
            EnumSet.of(TripStatus.PLANNED, TripStatus.ENROUTE, TripStatus.ENTERED);

    private final HaulTripRepository tripRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBagRepository appointmentBagRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final WasteBagRepository bagRepository;
    private final WastePointRepository pointRepository;
    private final AccessRuleService accessRuleService;
    private final CapacityService capacityService;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public DispatchService(HaulTripRepository tripRepository,
                           AppointmentRepository appointmentRepository,
                           AppointmentBagRepository appointmentBagRepository,
                           VehicleRepository vehicleRepository,
                           DriverRepository driverRepository,
                           WasteBagRepository bagRepository,
                           WastePointRepository pointRepository,
                           AccessRuleService accessRuleService,
                           CapacityService capacityService,
                           TraceService traceService,
                           EventPublisher publisher) {
        this.tripRepository = tripRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentBagRepository = appointmentBagRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.bagRepository = bagRepository;
        this.pointRepository = pointRepository;
        this.accessRuleService = accessRuleService;
        this.capacityService = capacityService;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    public record DispatchPlanCheck(List<String> errors, List<String> warnings, String suggestedTime) {}

    /** 派单前校验（不写库），供清运公司端预演方案 */
    @Transactional
    public DispatchPlanCheck preCheck(List<Long> appointmentIds, String plate, LocalDateTime plannedArrival) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Appointment> appts = loadAppointments(appointmentIds);

        int totalBags = appts.stream().mapToInt(Appointment::getBagCount).sum();
        java.math.BigDecimal totalKg = appts.stream()
                .map(Appointment::getEstimatedKg).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Vehicle vehicle = vehicleRepository.findByPlate(plate)
                .orElseThrow(() -> new BusinessException("车辆不存在: " + plate));
        if (Boolean.FALSE.equals(vehicle.getEnabled())) {
            errors.add("车辆 " + plate + " 已停用");
        }
        if (totalBags > vehicle.getCapacityBags()) {
            errors.add("车辆车厢容量不足：需装 " + totalBags + " 袋，车辆上限 " + vehicle.getCapacityBags() + " 袋");
        }
        if (totalKg.doubleValue() > vehicle.getCapacityKg()) {
            errors.add("车辆载重不足：预估 " + totalKg + "kg，核定 " + vehicle.getCapacityKg() + "kg");
        }

        // 门禁与尾号限行（按预约点位所属小区规则）
        String communityCode = resolveCommunity(appts);
        LocalDateTime arrival = plannedArrival != null ? plannedArrival
                : appts.stream().map(Appointment::getScheduledTime).min(LocalDateTime::compareTo).orElse(LocalDateTime.now());
        List<String> entryIssues = accessRuleService.checkEntry(communityCode, vehicle, arrival);
        errors.addAll(entryIssues);
        if (!entryIssues.isEmpty()) {
            LocalDateTime nextAllowed = accessRuleService.nextAllowedEntry(communityCode, vehicle, arrival);
            if (nextAllowed != null) {
                warnings.add("建议改派至下一可进场时间 " + nextAllowed + "（届时门禁/限行均放行）");
            }
        }

        // 已有派单冲突：同车在计划时间前后3小时存在活跃车次
        List<HaulTrip> samePlateActive = tripRepository.findByPlateAndStatusIn(plate, new ArrayList<>(ACTIVE_TRIP));
        for (HaulTrip t : samePlateActive) {
            if (t.getPlannedArrival() != null
                    && Math.abs(java.time.Duration.between(t.getPlannedArrival(), arrival).toHours()) < 3) {
                errors.add("车辆已有派单冲突：车次 " + t.getTripNo() + " 计划 " + t.getPlannedArrival());
            }
        }

        // 最晚清运时间约束
        for (Appointment a : appts) {
            if (arrival.isAfter(a.getDeadline())) {
                warnings.add("预约 " + a.getApptNo() + " 的到场时间晚于最晚清运时限 " + a.getDeadline()
                        + "，存在超时未清与垃圾点溢出风险");
            }
        }

        // 同点位已有待执行预约（容量/排队提醒）
        for (Appointment a : appts) {
            List<Appointment> queued = appointmentRepository
                    .findByPointIdAndStatusIn(a.getPointId(), List.of(AppointmentStatus.APPROVED, AppointmentStatus.DISPATCHED));
            long others = queued.stream().filter(q -> !q.getId().equals(a.getId())).count();
            if (others > 0) {
                warnings.add("点位 " + a.getPointId() + " 另有 " + others + " 个待清运预约，建议合理排序路线");
            }
        }
        return new DispatchPlanCheck(errors, warnings, null);
    }

    /** 正式派单 */
    @Transactional
    public HaulTrip dispatch(List<Long> appointmentIds, String plate, Long driverId,
                             LocalDateTime plannedArrival, String routePlan) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            throw new BusinessException("至少选择一个预约进行派单");
        }
        DispatchPlanCheck check = preCheck(appointmentIds, plate, plannedArrival);
        if (!check.errors().isEmpty()) {
            throw new BusinessException("派单校验未通过：" + String.join("；", check.errors()));
        }
        List<Appointment> appts = loadAppointments(appointmentIds);
        Vehicle vehicle = vehicleRepository.findByPlate(plate)
                .orElseThrow(() -> new BusinessException("车辆不存在: " + plate));
        Driver driver = driverId == null ? null
                : driverRepository.findById(driverId).orElseThrow(() -> new BusinessException("司机不存在: " + driverId));

        LocalDateTime arrival = plannedArrival != null ? plannedArrival
                : appts.stream().map(Appointment::getScheduledTime).min(LocalDateTime::compareTo).orElse(LocalDateTime.now());

        HaulTrip trip = new HaulTrip();
        trip.setTripNo(NoGenerator.next("TRIP"));
        trip.setManifestNo(NoGenerator.next("MF"));
        trip.setPlate(plate);
        trip.setVehicleId(vehicle.getId());
        if (driver != null) {
            trip.setDriverId(driver.getId());
            trip.setDriverName(driver.getName());
            trip.setDriverPhone(driver.getPhone());
        }
        trip.setPlannedArrival(arrival);
        trip.setRoutePlan(routePlan);
        trip.setAppointmentIds(appointmentIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        trip.setStatus(TripStatus.PLANNED);
        trip = tripRepository.save(trip);

        for (Appointment a : appts) {
            a.setStatus(AppointmentStatus.DISPATCHED);
            a.setTripId(trip.getId());
            appointmentRepository.save(a);
            for (AppointmentBag ab : appointmentBagRepository.findByAppointmentId(a.getId())) {
                WasteBag bag = bagRepository.findById(ab.getBagId()).orElse(null);
                if (bag != null && bag.getStatus() == BagStatus.SCHEDULED) {
                    bag.setTripId(trip.getId());
                    bagRepository.save(bag);
                }
            }
            traceService.record(TraceType.DISPATCHED, Role.COMPANY.name(),
                    "清运公司派单：车次 " + trip.getTripNo() + "，车牌 " + plate
                            + (driver != null ? "，司机 " + driver.getName() : "")
                            + "，计划到场 " + arrival + "，路线 " + routePlan,
                    a.getRegistrationId(), null, a.getId(), trip.getId(), null, null, null);
            publisher.publish("waste.dispatch.created",
                    DomainEvent.of("DISPATCHED", "车次 " + trip.getTripNo() + " 已派单")
                            .ref(a.getRegistrationId(), a.getId(), trip.getId())
                            .put("plate", plate)
                            .put("plannedArrival", arrival.toString()));
        }
        return trip;
    }

    /** 车辆出发 */
    @Transactional
    public HaulTrip enroute(Long tripId) {
        HaulTrip trip = mustGet(tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BusinessException("仅已排班车次可标记出发，当前 " + trip.getStatus());
        }
        trip.setStatus(TripStatus.ENROUTE);
        trip = tripRepository.save(trip);
        for (Long apptId : trip.appointmentIdList()) {
            traceService.record(TraceType.VEHICLE_ENROUTE, Role.COMPANY.name(),
                    "车辆 " + trip.getPlate() + " 已出发，预计 " + trip.getPlannedArrival() + " 到场",
                    null, null, apptId, trip.getId(), null, null, null);
        }
        return trip;
    }

    private List<Appointment> loadAppointments(List<Long> ids) {
        List<Appointment> appts = appointmentRepository.findAllById(ids);
        if (appts.size() != ids.size()) {
            throw new BusinessException("部分预约不存在");
        }
        for (Appointment a : appts) {
            if (a.getStatus() != AppointmentStatus.APPROVED) {
                throw new BusinessException("预约 " + a.getApptNo() + " 状态 " + a.getStatus()
                        + "，不能重复派单（如需改派请先取消原车次）");
            }
        }
        // 同一点位合并装车时，需按点位排序形成路线
        appts.sort((x, y) -> {
            int c = x.getPointId().compareTo(y.getPointId());
            return c != 0 ? c : x.getScheduledTime().compareTo(y.getScheduledTime());
        });
        return appts;
    }

    private String resolveCommunity(List<Appointment> appts) {
        return appts.stream()
                .map(a -> pointRepository.findById(a.getPointId()).map(WastePoint::getCommunityCode).orElse("DEFAULT"))
                .filter(c -> c != null && !"DEFAULT".equals(c))
                .findFirst()
                .orElse("DEFAULT");
    }

    public HaulTrip mustGet(Long id) {
        return tripRepository.findById(id).orElseThrow(() -> new BusinessException("车次不存在: " + id));
    }
}
