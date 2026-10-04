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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆进出场登记、装车核验、称重、称重异常检测。
 * 净重 = 出场重车重量 - 空车重量；净重按预约预估重量比例分摊到每袋，保证单袋可计费可追溯。
 */
@Service
public class WeighService {

    private final HaulTripRepository tripRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBagRepository appointmentBagRepository;
    private final WasteBagRepository bagRepository;
    private final WastePointRepository pointRepository;
    private final WeighAnomalyRepository anomalyRepository;
    private final ViolationService violationService;
    private final BillingService billingService;
    private final CapacityService capacityService;
    private final FeeService feeService;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public WeighService(HaulTripRepository tripRepository,
                        AppointmentRepository appointmentRepository,
                        AppointmentBagRepository appointmentBagRepository,
                        WasteBagRepository bagRepository,
                        WastePointRepository pointRepository,
                        WeighAnomalyRepository anomalyRepository,
                        ViolationService violationService,
                        BillingService billingService,
                        CapacityService capacityService,
                        FeeService feeService,
                        TraceService traceService,
                        EventPublisher publisher) {
        this.tripRepository = tripRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentBagRepository = appointmentBagRepository;
        this.bagRepository = bagRepository;
        this.pointRepository = pointRepository;
        this.anomalyRepository = anomalyRepository;
        this.violationService = violationService;
        this.billingService = billingService;
        this.capacityService = capacityService;
        this.feeService = feeService;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    /** 车辆进场：车牌、司机、空车重量、门禁记录 */
    @Transactional
    public HaulTrip vehicleEnter(Long tripId, BigDecimal tareWeight, String gateRecord,
                                 String photos, LocalDateTime enterTime) {
        HaulTrip trip = mustGet(tripId);
        if (trip.getStatus() != TripStatus.PLANNED && trip.getStatus() != TripStatus.ENROUTE) {
            throw new BusinessException("车次状态 " + trip.getStatus() + "，不能进场登记");
        }
        if (tareWeight == null || tareWeight.signum() <= 0) {
            throw new BusinessException("空车重量必须大于0");
        }
        trip.setStatus(TripStatus.ENTERED);
        trip.setTareWeight(tareWeight.setScale(2, RoundingMode.HALF_UP));
        trip.setEnterTime(enterTime != null ? enterTime : LocalDateTime.now());
        trip.setGateRecord(gateRecord);
        if (photos != null) trip.setPhotos(photos);
        trip = tripRepository.save(trip);

        for (Long apptId : trip.appointmentIdList()) {
            Appointment a = appointmentRepository.findById(apptId).orElse(null);
            if (a != null) {
                a.setStatus(AppointmentStatus.ARRIVED);
                appointmentRepository.save(a);
            }
            traceService.record(TraceType.VEHICLE_ENTER, Role.COMPANY.name(),
                    "车辆 " + trip.getPlate() + "（司机 " + trip.getDriverName() + "）进场，空车重 "
                            + trip.getTareWeight() + "kg，门禁记录：" + gateRecord,
                    a == null ? null : a.getRegistrationId(), null, apptId, trip.getId(),
                    null, null, null);
        }
        publisher.publish("waste.dispatch.arrived",
                DomainEvent.of("VEHICLE_ENTER", "车辆进场 " + trip.getPlate())
                        .ref(null, null, trip.getId()).put("tareKg", tareWeight));
        return trip;
    }

    /**
     * 装车核验 + 出场称重。
     * @param scannedBagCodes 现场扫码核验装车的袋编码（为空则按预约全部装车）
     */
    @Transactional
    public HaulTrip vehicleExit(Long tripId, BigDecimal grossWeight, Integer loadedBagCount,
                                List<String> scannedBagCodes, String photos, LocalDateTime exitTime) {
        HaulTrip trip = mustGet(tripId);
        if (trip.getStatus() != TripStatus.ENTERED) {
            throw new BusinessException("车辆未进场或已出场，当前状态 " + trip.getStatus());
        }
        if (trip.getTareWeight() == null) {
            throw new BusinessException("缺少空车重量，无法计算净重");
        }
        if (grossWeight == null || grossWeight.signum() <= 0) {
            throw new BusinessException("装载重量必须大于0");
        }
        grossWeight = grossWeight.setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = grossWeight.subtract(trip.getTareWeight());
        trip.setGrossWeight(grossWeight);
        trip.setNetWeight(net);
        trip.setExitTime(exitTime != null ? exitTime : LocalDateTime.now());
        if (photos != null) trip.setPhotos(photos);

        List<Appointment> appts = trip.appointmentIdList().stream()
                .map(id -> appointmentRepository.findById(id).orElseThrow()).toList();

        // —— 装车核验：扫码袋是否属于本车次预约 ——
        int plannedBags = 0;
        for (Appointment a : appts) plannedBags += a.getBagCount();
        int actualBags = loadedBagCount != null ? loadedBagCount : plannedBags;
        trip.setLoadedBagCount(actualBags);

        boolean bagCountMismatch = actualBags != plannedBags;
        if (bagCountMismatch) {
            openAnomaly(trip, "BAG_COUNT_MISMATCH",
                    "装车袋数与预约不符：预约 " + plannedBags + " 袋，实装清点 " + actualBags + " 袋",
                    BigDecimal.valueOf(plannedBags), BigDecimal.valueOf(actualBags), appts);
        }

        if (scannedBagCodes != null && !scannedBagCodes.isEmpty()) {
            for (String code : scannedBagCodes) {
                WasteBag bag = bagRepository.findByBagCode(code)
                        .orElseThrow(() -> new BusinessException("扫码袋不存在: " + code));
                boolean belongs = appts.stream().anyMatch(a -> a.getId().equals(bag.getAppointmentId()));
                if (!belongs) {
                    openAnomaly(trip, "BAG_COUNT_MISMATCH",
                            "装车袋 " + code + " 不属于本车次预约（疑似错装/串点装车）",
                            null, null, appts);
                }
            }
        }

        // —— 装车完成：袋状态推进为 LOADED ——
        for (Appointment a : appts) {
            for (AppointmentBag ab : appointmentBagRepository.findByAppointmentId(a.getId())) {
                WasteBag bag = bagRepository.findById(ab.getBagId()).orElse(null);
                if (bag != null && (bag.getStatus() == BagStatus.SCHEDULED || bag.getStatus() == BagStatus.ACCEPTED)) {
                    bag.setStatus(BagStatus.LOADED);
                    bag.setTripId(trip.getId());
                    bag.setLoadedAt(trip.getExitTime());
                    bagRepository.save(bag);
                }
            }
            a.setStatus(AppointmentStatus.LOADED);
            appointmentRepository.save(a);
        }

        // —— 称重异常检测 ——
        if (net.signum() <= 0) {
            openAnomaly(trip, "NET_NEGATIVE",
                    "净重异常：空车 " + trip.getTareWeight() + "kg ≥ 重车 " + grossWeight
                            + "kg，净重 " + net + "kg，请核磅",
                    grossWeight, trip.getTareWeight(), appts);
            // 净重异常时账单挂起，等待人工裁决结算重量
            trip.setStatus(TripStatus.EXITED);
            trip = tripRepository.save(trip);
            publisher.publish("waste.weigh.anomaly",
                    DomainEvent.of("WEIGH_ANOMALY", "净重异常挂起：" + trip.getTripNo())
                            .ref(null, null, trip.getId()).put("type", "NET_NEGATIVE"));
            return trip;
        }

        BigDecimal estTotal = appts.stream().map(Appointment::getEstimatedKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double deviationLimit = feeService.get(FeeService.WEIGH_DEVIATION_LIMIT, BigDecimal.valueOf(0.35)).doubleValue();
        if (estTotal.signum() > 0) {
            double deviation = net.subtract(estTotal).abs().divide(estTotal, 4, RoundingMode.HALF_UP).doubleValue();
            if (deviation > deviationLimit) {
                openAnomaly(trip, "DEVIATION",
                        String.format("称重与预估偏差 %.0f%%（超 %.0f%%）：预估 %.2fkg，实称 %.2fkg",
                                deviation * 100, deviationLimit * 100, estTotal, net),
                        estTotal, net, appts);
            }
        }

        // —— 净重按各预约预估重量比例分摊，再落到袋 ——
        for (Appointment a : appts) {
            BigDecimal share = estTotal.signum() == 0 ? BigDecimal.ZERO
                    : net.multiply(a.getEstimatedKg()).divide(estTotal, 2, RoundingMode.HALF_UP);
            allocateWeightToBags(a, share);
        }

        // 车辆迟到判定（相对最早预约 deadline）
        LocalDateTime earliestDeadline = appts.stream().map(Appointment::getDeadline)
                .min(LocalDateTime::compareTo).orElse(null);
        LocalDateTime effectiveArrival = trip.getEnterTime();
        boolean late = false;
        if (earliestDeadline != null && effectiveArrival != null && effectiveArrival.isAfter(earliestDeadline)) {
            int lateMinutes = (int) Duration.between(earliestDeadline, effectiveArrival).toMinutes();
            late = true;
            for (Appointment a : appts) {
                a.setLateMinutes(lateMinutes);
                appointmentRepository.save(a);
            }
            publisher.publish("waste.trip.late",
                    DomainEvent.of("TRIP_LATE", "车辆迟到 " + lateMinutes + " 分钟")
                            .ref(null, null, trip.getId())
                            .put("lateMinutes", lateMinutes)
                            .put("appointmentIds", trip.getAppointmentIds()));
        }

        trip.setStatus(TripStatus.EXITED);
        trip = tripRepository.save(trip);

        // 称重有效：生成业主清运费账单与清运公司结算单
        billingService.generateTripBilling(trip, appts);

        for (Appointment a : appts) {
            a.setStatus(AppointmentStatus.LOADED);
            appointmentRepository.save(a);
            traceService.record(TraceType.VEHICLE_EXIT, Role.COMPANY.name(),
                    "车辆出场称重：重车 " + grossWeight + "kg，空车 " + trip.getTareWeight()
                            + "kg，净重 " + net + "kg，装车 " + actualBags + " 袋，清运联单 "
                            + trip.getManifestNo() + (late ? "；车辆迟到 " + a.getLateMinutes() + " 分钟" : ""),
                    a.getRegistrationId(), null, a.getId(), trip.getId(), null, null, null);
            for (AppointmentBag ab : appointmentBagRepository.findByAppointmentId(a.getId())) {
                capacityService.refreshPointStatus(a.getPointId());
            }
        }
        publisher.publish("waste.weigh.anomaly", DomainEvent.of("WEIGH_DONE", "称重完成")
                .ref(null, null, trip.getId()).put("netKg", net));
        return trip;
    }

    private void allocateWeightToBags(Appointment a, BigDecimal appointmentNet) {
        List<AppointmentBag> abs = appointmentBagRepository.findByAppointmentId(a.getId());
        if (abs.isEmpty()) return;
        BigDecimal perBag = appointmentNet.divide(BigDecimal.valueOf(abs.size()), 2, RoundingMode.HALF_UP);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < abs.size(); i++) {
            WasteBag bag = bagRepository.findById(abs.get(i).getBagId()).orElse(null);
            if (bag == null) continue;
            BigDecimal w = (i == abs.size() - 1) ? appointmentNet.subtract(allocated) : perBag;
            bag.setActualKg(w);
            bag.setStatus(BagStatus.WEIGHED);
            bagRepository.save(bag);
            traceService.record(TraceType.VEHICLE_EXIT, Role.SYSTEM.name(),
                    "袋 " + bag.getBagCode() + " 分摊净重 " + w + "kg（按预约预估重量比例分摊车次净重）",
                    bag.getRegistration().getId(), bag.getBagCode(), a.getId(), bag.getTripId(),
                    null, null, null);
            allocated = allocated.add(w);
        }
    }

    private WeighAnomaly openAnomaly(HaulTrip trip, String type, String detail,
                                     BigDecimal expected, BigDecimal actual, List<Appointment> appts) {
        WeighAnomaly anomaly = new WeighAnomaly();
        anomaly.setAnomalyNo(NoGenerator.next("ANM"));
        anomaly.setTripId(trip.getId());
        anomaly.setAppointmentId(appts.isEmpty() ? null : appts.get(0).getId());
        anomaly.setType(type);
        anomaly.setDetail(detail);
        anomaly.setExpectedValue(expected);
        anomaly.setActualValue(actual);
        anomaly.setStatus(AnomalyStatus.OPEN);
        anomaly = anomalyRepository.save(anomaly);
        traceService.record(TraceType.WEIGH_ANOMALY, Role.SYSTEM.name(),
                "称重异常[" + anomaly.getAnomalyNo() + "]：" + detail,
                appts.isEmpty() ? null : appts.get(0).getRegistrationId(),
                null, appts.isEmpty() ? null : appts.get(0).getId(), trip.getId(), null, null, null);
        publisher.publish("waste.weigh.anomaly",
                DomainEvent.of("WEIGH_ANOMALY", detail).ref(null,
                        appts.isEmpty() ? null : appts.get(0).getId(), trip.getId())
                        .put("anomalyId", anomaly.getId()).put("type", type));
        return anomaly;
    }

    /** 人工处理称重异常：确认正常 / 调整结算重量 */
    @Transactional
    public WeighAnomaly resolveAnomaly(Long anomalyId, AnomalyStatus decision,
                                       BigDecimal resolvedWeight, String resolution, Long operatorId) {
        WeighAnomaly a = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new BusinessException("称重异常不存在: " + anomalyId));
        if (a.getStatus() != AnomalyStatus.OPEN) {
            throw new BusinessException("异常已处理，当前 " + a.getStatus());
        }
        a.setStatus(decision);
        a.setHandledBy(operatorId);
        a.setHandledAt(LocalDateTime.now());
        a.setResolution(resolution);
        if (decision == AnomalyStatus.ADJUSTED) {
            if (resolvedWeight == null || resolvedWeight.signum() <= 0) {
                throw new BusinessException("调整异常必须提供有效的结算重量");
            }
            a.setResolvedWeight(resolvedWeight);
        }
        return anomalyRepository.save(a);
    }

    private HaulTrip mustGet(Long id) {
        return tripRepository.findById(id).orElseThrow(() -> new BusinessException("车次不存在: " + id));
    }
}
