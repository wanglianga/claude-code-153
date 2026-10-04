package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.Complaint;
import com.community.waste.entity.Violation;
import com.community.waste.mq.DomainEvent;
import com.community.waste.mq.EventPublisher;
import com.community.waste.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邻居投诉受理与处理：异味/扬尘/违规堆放等。
 * 处理时可关联违规认定与罚款账单，形成“投诉→核查→追责→费用”闭环。
 */
@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ViolationService violationService;
    private final FeeService feeService;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ViolationService violationService,
                            FeeService feeService,
                            TraceService traceService,
                            EventPublisher publisher) {
        this.complaintRepository = complaintRepository;
        this.violationService = violationService;
        this.feeService = feeService;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    @Transactional
    public Complaint submit(Complaint c) {
        c.setComplaintNo(NoGenerator.next("CMP"));
        c.setStatus(ComplaintStatus.SUBMITTED);
        c = complaintRepository.save(c);
        publisher.publish("waste.complaint.submitted",
                DomainEvent.of("COMPLAINT_SUBMITTED", c.getType() + "：" + c.getContent())
                        .ref(c.getRegistrationId(), c.getAppointmentId(), null)
                        .put("complaintId", c.getId()));
        traceService.record(TraceType.COMPLAINT, Role.OWNER.name(),
                "邻居投诉[" + c.getComplaintNo() + "]（" + c.getType().label + "）来自 "
                        + c.getComplainantLocation() + "：" + c.getContent(),
                c.getRegistrationId(), null, c.getAppointmentId(), null, null, null, c.getId());
        return c;
    }

    @Transactional
    public Complaint investigate(Long id, Long operatorId) {
        Complaint c = mustGet(id);
        c.setStatus(ComplaintStatus.INVESTIGATING);
        c.setHandledBy(operatorId);
        return complaintRepository.save(c);
    }

    /**
     * 处理投诉：
     * @param openViolation 是否同时登记违规（责任方 CREW/COMPANY/OWNER）
     * @param fineAmount 罚款金额（为 null 按投诉类型取默认）
     */
    @Transactional
    public Complaint resolve(Long id, String result, String liableParty, boolean openViolation,
                             ViolationType violationType, BigDecimal fineAmount, boolean confirmNow,
                             Long operatorId) {
        Complaint c = mustGet(id);
        c.setStatus(ComplaintStatus.RESOLVED);
        c.setHandledBy(operatorId);
        c.setHandledAt(LocalDateTime.now());
        c.setHandleResult(result);
        c.setLiableParty(liableParty);

        if (openViolation) {
            ViolationType type = violationType != null ? violationType : mapType(c.getType());
            BigDecimal fine = fineAmount != null ? fineAmount : defaultFine(type);
            Violation v = violationService.open(type, ViolationSource.COMPLAINT,
                    "由投诉[" + c.getComplaintNo() + "]核查认定：" + c.getContent() + "；处理：" + result,
                    c.getRegistrationId(), c.getAppointmentId(), null, c.getId(),
                    liableParty == null ? "CREW" : liableParty, fine, c.getEvidence(), false);
            c.setViolationId(v.getId());
            if (confirmNow) {
                v = violationService.confirm(v.getId(), operatorId);
                c.setBillId(v.getBillId());
            }
        }
        c = complaintRepository.save(c);
        traceService.record(TraceType.COMPLAINT_RESOLVED, Role.PROPERTY.name(),
                "投诉[" + c.getComplaintNo() + "]处理完成：" + result
                        + (liableParty == null ? "" : "；责任方 " + liableParty),
                c.getRegistrationId(), null, c.getAppointmentId(), null,
                c.getBillId(), c.getViolationId(), c.getId());
        publisher.publish("waste.complaint.resolved",
                DomainEvent.of("COMPLAINT_RESOLVED", result)
                        .ref(c.getRegistrationId(), c.getAppointmentId(), null)
                        .put("complaintId", c.getId()));
        return c;
    }

    @Transactional
    public Complaint reject(Long id, String reason, Long operatorId) {
        Complaint c = mustGet(id);
        c.setStatus(ComplaintStatus.REJECTED);
        c.setHandledBy(operatorId);
        c.setHandledAt(LocalDateTime.now());
        c.setHandleResult("驳回：" + reason);
        return complaintRepository.save(c);
    }

    private ViolationType mapType(ComplaintType type) {
        return switch (type) {
            case ILLEGAL_PILE -> ViolationType.ILLEGAL_LOCATION;
            case MIXED -> ViolationType.MIXED_HOUSEHOLD;
            default -> ViolationType.OTHER;
        };
    }

    private BigDecimal defaultFine(ViolationType type) {
        return switch (type) {
            case FIRE_LANE -> BigDecimal.valueOf(500);
            case MIXED_HOUSEHOLD -> BigDecimal.valueOf(300);
            case ILLEGAL_LOCATION -> BigDecimal.valueOf(200);
            default -> BigDecimal.valueOf(100);
        };
    }

    public Complaint mustGet(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new BusinessException("投诉不存在: " + id));
    }
}
