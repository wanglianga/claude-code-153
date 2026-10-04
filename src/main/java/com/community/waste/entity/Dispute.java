package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.DisputeStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

/** 费用争议 */
@Entity
@Table(name = "dispute", indexes = @Index(name = "idx_dispute_status", columnList = "status"))
public class Dispute extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String disputeNo;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    private Long registrationId;

    /** 提出方角色 OWNER/CREW/COMPANY */
    @Column(nullable = false, length = 16)
    private String raisedByRole;

    @Column(length = 32)
    private String raisedByName;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(length = 512)
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DisputeStatus status = DisputeStatus.OPEN;

    /** 裁决调整金额（负数减收） */
    @Column(precision = 12, scale = 2)
    private BigDecimal adjustment = BigDecimal.ZERO;

    private Long reviewedBy;
    private java.time.LocalDateTime reviewedAt;

    @Column(length = 512)
    private String reviewResult;

    public String getDisputeNo() { return disputeNo; }
    public void setDisputeNo(String disputeNo) { this.disputeNo = disputeNo; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public String getRaisedByRole() { return raisedByRole; }
    public void setRaisedByRole(String raisedByRole) { this.raisedByRole = raisedByRole; }
    public String getRaisedByName() { return raisedByName; }
    public void setRaisedByName(String raisedByName) { this.raisedByName = raisedByName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
    public BigDecimal getAdjustment() { return adjustment; }
    public void setAdjustment(BigDecimal adjustment) { this.adjustment = adjustment; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public java.time.LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(java.time.LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewResult() { return reviewResult; }
    public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
}
