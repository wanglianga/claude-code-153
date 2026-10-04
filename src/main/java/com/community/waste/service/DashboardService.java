package com.community.waste.service;

import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.*;
import com.community.waste.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物业端 / 清运公司端看板：
 * 各楼栋预约、超时未清、违规堆放、押金扣款、清运公司履约；派单路线、车辆容量、称重回传、司机到场。
 */
@Service
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final RegistrationRepository registrationRepository;
    private final ViolationRepository violationRepository;
    private final DepositDeductionRepository deductionRepository;
    private final HaulTripRepository tripRepository;
    private final BillRepository billRepository;
    private final ComplaintRepository complaintRepository;
    private final WasteBagRepository bagRepository;
    private final VehicleRepository vehicleRepository;
    private final CapacityService capacityService;

    public DashboardService(AppointmentRepository appointmentRepository,
                            RegistrationRepository registrationRepository,
                            ViolationRepository violationRepository,
                            DepositDeductionRepository deductionRepository,
                            HaulTripRepository tripRepository,
                            BillRepository billRepository,
                            ComplaintRepository complaintRepository,
                            WasteBagRepository bagRepository,
                            VehicleRepository vehicleRepository,
                            CapacityService capacityService) {
        this.appointmentRepository = appointmentRepository;
        this.registrationRepository = registrationRepository;
        this.violationRepository = violationRepository;
        this.deductionRepository = deductionRepository;
        this.tripRepository = tripRepository;
        this.billRepository = billRepository;
        this.complaintRepository = complaintRepository;
        this.bagRepository = bagRepository;
        this.vehicleRepository = vehicleRepository;
        this.capacityService = capacityService;
    }

    /** 物业端总览 */
    @Transactional(readOnly = true)
    public Map<String, Object> propertyOverview() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<AppointmentStatus> open = List.of(
                AppointmentStatus.APPROVED, AppointmentStatus.DISPATCHED, AppointmentStatus.ARRIVED, AppointmentStatus.LOADED);
        List<Appointment> activeAppts = appointmentRepository.findByStatusIn(open);

        // 按楼栋聚合
        Map<String, Map<String, Object>> byBuilding = new LinkedHashMap<>();
        List<Map<String, Object>> overdue = new ArrayList<>();
        for (Appointment a : activeAppts) {
            Registration reg = registrationRepository.findById(a.getRegistrationId()).orElse(null);
            String building = reg == null ? "未知楼栋" : reg.getBuildingNo();
            byBuilding.computeIfAbsent(building, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("buildingNo", k);
                m.put("activeAppointments", 0);
                m.put("pendingBags", 0);
                return m;
            });
            Map<String, Object> bm = byBuilding.get(building);
            bm.put("activeAppointments", (int) bm.get("activeAppointments") + 1);
            bm.put("pendingBags", (int) bm.get("pendingBags") + a.getBagCount());

            if (a.getStatus() != AppointmentStatus.LOADED
                    && a.getDeadline() != null && LocalDateTime.now().isAfter(a.getDeadline())) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("appointmentId", a.getId());
                o.put("apptNo", a.getApptNo());
                o.put("buildingNo", building);
                o.put("roomNo", reg == null ? null : reg.getRoomNo());
                o.put("deadline", a.getDeadline());
                o.put("lateMinutes", a.getLateMinutes());
                o.put("spillOccurred", a.getSpillOccurred());
                o.put("status", a.getStatus());
                overdue.add(o);
            }
        }

        List<Map<String, Object>> violations = violationRepository.findByStatus(ViolationStatus.OPEN).stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("violationId", v.getId());
                    m.put("violationNo", v.getViolationNo());
                    m.put("type", v.getType());
                    m.put("liableParty", v.getLiableParty());
                    m.put("description", v.getDescription());
                    Registration reg = v.getRegistrationId() == null ? null
                            : registrationRepository.findById(v.getRegistrationId()).orElse(null);
                    m.put("roomNo", reg == null ? null : reg.getBuildingNo() + "-" + reg.getRoomNo());
                    return m;
                }).toList();

        long complaintOpen = complaintRepository.findByStatus(ComplaintStatus.SUBMITTED).size()
                + complaintRepository.findByStatus(ComplaintStatus.INVESTIGATING).size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeAppointments", activeAppts.size());
        summary.put("overdueCount", overdue.size());
        summary.put("openViolations", violations.size());
        summary.put("openComplaints", complaintOpen);
        summary.put("openDisputedBills", billRepository.findByStatus(BillStatus.DISPUTED).size());

        data.put("summary", summary);
        data.put("byBuilding", new ArrayList<>(byBuilding.values()));
        data.put("overdueAppointments", overdue);
        data.put("openViolations", violations);
        return data;
    }

    /** 押金扣款明细 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> depositDeductions(Long registrationId) {
        List<DepositDeduction> list = registrationId == null
                ? deductionRepository.findAll() : deductionRepository.findByRegistrationIdOrderByCreatedAtAsc(registrationId);
        return list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deductionNo", d.getDeductionNo());
            m.put("registrationId", d.getRegistrationId());
            m.put("amount", d.getAmount());
            m.put("reasonType", d.getReasonType());
            m.put("reason", d.getReason());
            m.put("balanceAfter", d.getBalanceAfter());
            m.put("createdAt", d.getCreatedAt());
            return m;
        }).toList();
    }

    /** 清运公司履约统计 */
    @Transactional(readOnly = true)
    public Map<String, Object> companyPerformance() {
        List<HaulTrip> all = tripRepository.findAll();
        long onTime = all.stream().filter(t -> t.getStatus() == TripStatus.RECEIVED).count();
        long lateTrips = all.stream().filter(t -> {
            if (t.getPlannedArrival() == null || t.getEnterTime() == null) return false;
            return t.getEnterTime().isAfter(t.getPlannedArrival().plusMinutes(30));
        }).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTrips", all.size());
        data.put("receivedTrips", onTime);
        data.put("lateTrips", lateTrips);

        // 公司侧违约罚款
        java.math.BigDecimal fines = violationRepository.findByStatus(ViolationStatus.CONFIRMED).stream()
                .filter(v -> "COMPANY".equals(v.getLiableParty()))
                .map(Violation::getFineAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        data.put("companyFines", fines);

        // 待派单预约
        data.put("pendingDispatch",
                appointmentRepository.findByStatusIn(List.of(AppointmentStatus.APPROVED)).size());
        return data;
    }

    /** 清运公司端：车辆与活跃派单 */
    @Transactional(readOnly = true)
    public Map<String, Object> companyBoard() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> vehicles = vehicleRepository.findByEnabledTrue().stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vehicleId", v.getId());
            m.put("plate", v.getPlate());
            m.put("tailDigit", v.getTailDigit());
            m.put("capacityBags", v.getCapacityBags());
            m.put("capacityKg", v.getCapacityKg());
            List<HaulTrip> active = tripRepository.findByPlateAndStatusIn(v.getPlate(),
                    List.of(TripStatus.PLANNED, TripStatus.ENROUTE, TripStatus.ENTERED));
            m.put("activeTrips", active.stream().map(t -> {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("tripId", t.getId());
                tm.put("tripNo", t.getTripNo());
                tm.put("status", t.getStatus());
                tm.put("driverName", t.getDriverName());
                tm.put("driverPhone", t.getDriverPhone());
                tm.put("plannedArrival", t.getPlannedArrival());
                tm.put("enterTime", t.getEnterTime());
                tm.put("routePlan", t.getRoutePlan());
                tm.put("appointmentIds", t.appointmentIdList());
                tm.put("manifestNo", t.getManifestNo());
                return tm;
            }).toList());
            return m;
        }).toList();
        data.put("vehicles", vehicles);

        List<Appointment> pending = appointmentRepository.findByStatusIn(List.of(AppointmentStatus.APPROVED));
        data.put("pendingAppointments", pending.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("appointmentId", a.getId());
            m.put("apptNo", a.getApptNo());
            m.put("pointId", a.getPointId());
            m.put("bagCount", a.getBagCount());
            m.put("estimatedKg", a.getEstimatedKg());
            m.put("scheduledTime", a.getScheduledTime());
            m.put("deadline", a.getDeadline());
            return m;
        }).toList());
        return data;
    }

    /** 财务端账单总览 */
    @Transactional(readOnly = true)
    public Map<String, Object> financeOverview() {
        Map<String, Object> data = new LinkedHashMap<>();
        for (BillPartyType pt : BillPartyType.values()) {
            Map<String, Object> agg = new LinkedHashMap<>();
            List<Bill> bills = billRepository.findAll().stream()
                    .filter(b -> b.getPartyType() == pt).toList();
            java.math.BigDecimal receivable = bills.stream().filter(Bill::getReceivable)
                    .map(Bill::payableAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal payable = bills.stream().filter(b -> !b.getReceivable())
                    .map(Bill::payableAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            long unpaid = bills.stream().filter(b -> b.getStatus() == BillStatus.UNPAID
                    || b.getStatus() == BillStatus.HELD).count();
            agg.put("count", bills.size());
            agg.put("receivable", receivable);
            agg.put("payable", payable);
            agg.put("unpaidOrHeld", unpaid);
            data.put(pt.name(), agg);
        }
        return data;
    }
}
