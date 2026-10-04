package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.ViolationSource;
import com.community.waste.domain.DomainEnums.ViolationStatus;
import com.community.waste.domain.DomainEnums.ViolationType;
import jakarta.persistence.*;
import java.math.BigDecimal;

/** 违规记录（消防通道堆放、混入生活垃圾、溢出等） */
@Entity
@Table(name = "violation", indexes = @Index(name = "idx_violation_status", columnList = "status"))
public class Violation extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String violationNo;

    @Column(name = "registration_id")
    private Long registrationId;

    private Long appointmentId;
    private Long tripId;
    private Long complaintId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ViolationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ViolationSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ViolationStatus status = ViolationStatus.OPEN;

    /** 责任主体：CREW 施工队 / COMPANY 清运公司 / OWNER 业主 */
    @Column(nullable = false, length = 16)
    private String liableParty = "CREW";

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 512)
    private String evidence;

    /** 罚款金额 */
    @Column(precision = 12, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    /** 认定人（物业） */
    private Long confirmedBy;
    private java.time.LocalDateTime confirmedAt;

    /** 关联扣款单与账单 */
    private Long deductionId;
    private Long billId;

    public String getViolationNo() { return violationNo; }
    public void setViolationNo(String violationNo) { this.violationNo = violationNo; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public ViolationType getType() { return type; }
    public void setType(ViolationType type) { this.type = type; }
    public ViolationSource getSource() { return source; }
    public void setSource(ViolationSource source) { this.source = source; }
    public ViolationStatus getStatus() { return status; }
    public void setStatus(ViolationStatus status) { this.status = status; }
    public String getLiableParty() { return liableParty; }
    public void setLiableParty(String liableParty) { this.liableParty = liableParty; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
    public java.time.LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(java.time.LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public Long getDeductionId() { return deductionId; }
    public void setDeductionId(Long deductionId) { this.deductionId = deductionId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
}
