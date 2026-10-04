package com.community.waste.service;

import com.community.waste.domain.DomainEnums.BagStatus;
import com.community.waste.entity.*;
import com.community.waste.repository.*;
import com.community.waste.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 投诉追责还原：按袋编码 / 房号（装修登记）还原
 * “谁堆放、谁审核、谁清走、车辆车牌司机、称重多少、处置回执、费用如何产生、违规与投诉”。
 */
@Service
public class TrackingService {

    private final TraceEventRepository traceRepository;
    private final WasteBagRepository bagRepository;
    private final RegistrationRepository registrationRepository;
    private final DailyReportRepository reportRepository;
    private final AppointmentRepository appointmentRepository;
    private final HaulTripRepository tripRepository;
    private final DisposalReceiptRepository receiptRepository;
    private final BillRepository billRepository;
    private final ViolationRepository violationRepository;
    private final ComplaintRepository complaintRepository;
    private final DepositDeductionRepository deductionRepository;

    public TrackingService(TraceEventRepository traceRepository, WasteBagRepository bagRepository,
                           RegistrationRepository registrationRepository, DailyReportRepository reportRepository,
                           AppointmentRepository appointmentRepository, HaulTripRepository tripRepository,
                           DisposalReceiptRepository receiptRepository, BillRepository billRepository,
                           ViolationRepository violationRepository, ComplaintRepository complaintRepository,
                           DepositDeductionRepository deductionRepository) {
        this.traceRepository = traceRepository;
        this.bagRepository = bagRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.appointmentRepository = appointmentRepository;
        this.tripRepository = tripRepository;
        this.receiptRepository = receiptRepository;
        this.billRepository = billRepository;
        this.violationRepository = violationRepository;
        this.complaintRepository = complaintRepository;
        this.deductionRepository = deductionRepository;
    }

    /** 按单袋还原 */
    @Transactional(readOnly = true)
    public Map<String, Object> traceBag(String bagCode) {
        WasteBag bag = bagRepository.findByBagCode(bagCode)
                .orElseThrow(() -> new BusinessException("垃圾袋不存在: " + bagCode));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bag", bagInfo(bag));
        result.put("timeline", traceRepository.findByBagCodeOrderByCreatedAtAsc(bagCode).stream()
                .map(this::eventInfo).toList());

        Map<String, Object> links = new LinkedHashMap<>();
        if (bag.getAppointmentId() != null) {
            appointmentRepository.findById(bag.getAppointmentId()).ifPresent(a -> links.put("appointment", apptInfo(a)));
        }
        if (bag.getTripId() != null) {
            tripRepository.findById(bag.getTripId()).ifPresent(t -> {
                links.put("trip", tripInfo(t));
                receiptRepository.findByTripId(t.getId()).ifPresent(r -> links.put("disposalReceipt", receiptInfo(r)));
            });
        }
        links.put("bills", billRepository.findByRegistrationId(bag.getRegistration().getId()).stream()
                .filter(b -> b.getTripId() != null && b.getTripId().equals(bag.getTripId()))
                .map(this::billInfo).toList());
        result.put("links", links);
        return result;
    }

    /** 按装修登记（房号）还原全部袋与事件 */
    @Transactional(readOnly = true)
    public Map<String, Object> traceRegistration(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + registrationId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registration", Map.of(
                "registrationId", reg.getId(),
                "roomNo", reg.getBuildingNo() + "-" + reg.getRoomNo(),
                "ownerName", reg.getOwnerName(),
                "crewName", reg.getCrewName(),
                "deposit", reg.getDeposit(),
                "depositDeducted", reg.getDepositDeducted(),
                "status", reg.getStatus()));
        result.put("timeline", traceRepository.findByRegistrationIdOrderByCreatedAtAsc(registrationId).stream()
                .map(this::eventInfo).toList());
        result.put("bags", bagRepository.findByRegistrationIdOrderByCreatedAtAsc(registrationId).stream()
                .map(this::bagInfo).toList());
        result.put("appointments", appointmentRepository.findByRegistrationId(registrationId).stream()
                .map(this::apptInfo).toList());
        result.put("bills", billRepository.findByRegistrationId(registrationId).stream()
                .map(this::billInfo).toList());
        result.put("violations", violationRepository.findByRegistrationId(registrationId).stream()
                .map(this::violationInfo).toList());
        result.put("complaints", complaintRepository.findByRegistrationId(registrationId).stream()
                .map(this::complaintInfo).toList());
        result.put("depositDeductions",
                deductionRepository.findByRegistrationIdOrderByCreatedAtAsc(registrationId).stream()
                        .map(d -> Map.of("deductionNo", d.getDeductionNo(), "amount", d.getAmount(),
                                "reason", d.getReason(), "reasonType", d.getReasonType(),
                                "balanceAfter", d.getBalanceAfter(), "createdAt", d.getCreatedAt()))
                        .toList());
        return result;
    }

    /** 投诉追责视图：投诉 -> 关联登记的完整责任链 */
    @Transactional(readOnly = true)
    public Map<String, Object> accountabilityForComplaint(Long complaintId) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new BusinessException("投诉不存在: " + complaintId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("complaint", complaintInfo(c));
        if (c.getRegistrationId() != null) {
            result.put("chain", traceRegistration(c.getRegistrationId()));
        }
        if (c.getViolationId() != null) {
            result.put("violation", violationRepository.findById(c.getViolationId()).map(this::violationInfo).orElse(null));
        }
        return result;
    }

    private Map<String, Object> eventInfo(TraceEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", e.getCreatedAt());
        m.put("type", e.getType());
        m.put("typeLabel", e.getType().label);
        m.put("actorRole", e.getActorRole());
        m.put("actorName", e.getActorName());
        m.put("detail", e.getDetail());
        m.put("bagCode", e.getBagCode());
        m.put("appointmentId", e.getAppointmentId());
        m.put("tripId", e.getTripId());
        m.put("billId", e.getBillId());
        m.put("violationId", e.getViolationId());
        m.put("complaintId", e.getComplaintId());
        return m;
    }

    private Map<String, Object> bagInfo(WasteBag b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bagId", b.getId());
        m.put("bagCode", b.getBagCode());
        m.put("status", b.getStatus());
        m.put("statusLabel", b.getStatus().label);
        m.put("category", b.getCategory());
        m.put("extra", b.getExtra());
        m.put("estimatedKg", b.getEstimatedKg());
        m.put("actualKg", b.getActualKg());
        m.put("appointmentId", b.getAppointmentId());
        m.put("tripId", b.getTripId());
        m.put("acceptedBy", b.getAcceptedBy());
        m.put("acceptedAt", b.getAcceptedAt());
        m.put("loadedAt", b.getLoadedAt());
        m.put("disposedAt", b.getDisposedAt());
        return m;
    }

    private Map<String, Object> apptInfo(Appointment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appointmentId", a.getId());
        m.put("apptNo", a.getApptNo());
        m.put("pointId", a.getPointId());
        m.put("bagCount", a.getBagCount());
        m.put("estimatedKg", a.getEstimatedKg());
        m.put("scheduledTime", a.getScheduledTime());
        m.put("deadline", a.getDeadline());
        m.put("status", a.getStatus());
        m.put("statusLabel", a.getStatus().label);
        m.put("tripId", a.getTripId());
        m.put("lateMinutes", a.getLateMinutes());
        m.put("spillOccurred", a.getSpillOccurred());
        return m;
    }

    private Map<String, Object> tripInfo(HaulTrip t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tripId", t.getId());
        m.put("tripNo", t.getTripNo());
        m.put("manifestNo", t.getManifestNo());
        m.put("plate", t.getPlate());
        m.put("driverName", t.getDriverName());
        m.put("driverPhone", t.getDriverPhone());
        m.put("plannedArrival", t.getPlannedArrival());
        m.put("enterTime", t.getEnterTime());
        m.put("exitTime", t.getExitTime());
        m.put("tareWeight", t.getTareWeight());
        m.put("grossWeight", t.getGrossWeight());
        m.put("netWeight", t.getNetWeight());
        m.put("loadedBagCount", t.getLoadedBagCount());
        m.put("gateRecord", t.getGateRecord());
        m.put("photos", t.getPhotos());
        m.put("status", t.getStatus());
        return m;
    }

    private Map<String, Object> receiptInfo(DisposalReceipt r) {
        return Map.of("receiptNo", r.getReceiptNo(), "siteName", r.getSiteName(),
                "receivedWeight", r.getReceivedWeight(), "unitPrice", r.getUnitPrice(),
                "disposalFee", r.getDisposalFee(), "receivedAt", r.getReceivedAt(),
                "operator", r.getOperator() == null ? "" : r.getOperator());
    }

    private Map<String, Object> billInfo(Bill b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("billId", b.getId());
        m.put("billNo", b.getBillNo());
        m.put("billType", b.getBillType());
        m.put("partyType", b.getPartyType());
        m.put("amount", b.getAmount());
        m.put("adjustment", b.getAdjustment());
        m.put("payableAmount", b.payableAmount());
        m.put("status", b.getStatus());
        m.put("receivable", b.getReceivable());
        m.put("summary", b.getSummary());
        m.put("weightKg", b.getWeightKg());
        m.put("tripId", b.getTripId());
        return m;
    }

    private Map<String, Object> violationInfo(Violation v) {
        return Map.of("violationId", v.getId(), "violationNo", v.getViolationNo(), "type", v.getType(),
                "typeLabel", v.getType().label, "status", v.getStatus(), "liableParty", v.getLiableParty(),
                "fineAmount", v.getFineAmount(), "description", v.getDescription(),
                "billId", v.getBillId() == null ? 0L : v.getBillId());
    }

    private Map<String, Object> complaintInfo(Complaint c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("complaintId", c.getId());
        m.put("complaintNo", c.getComplaintNo());
        m.put("type", c.getType());
        m.put("typeLabel", c.getType().label);
        m.put("content", c.getContent());
        m.put("status", c.getStatus());
        m.put("complainantLocation", c.getComplainantLocation());
        m.put("liableParty", c.getLiableParty());
        m.put("handleResult", c.getHandleResult());
        m.put("violationId", c.getViolationId());
        return m;
    }
}
