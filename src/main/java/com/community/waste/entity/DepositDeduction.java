package com.community.waste.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 押金扣款记录 */
@Entity
@Table(name = "deposit_deduction",
        indexes = @Index(name = "idx_deduction_reg", columnList = "registration_id"))
public class DepositDeduction extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String deductionNo;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    /** 扣款原因类型：VIOLATION 违规 / DISPOSAL 清运欠费 / MANUAL 人工 */
    @Column(nullable = false, length = 16)
    private String reasonType;

    private Long violationId;
    private Long billId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason;

    private Long createdBy;

    /** 扣款后押金余额 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    public String getDeductionNo() { return deductionNo; }
    public void setDeductionNo(String deductionNo) { this.deductionNo = deductionNo; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public String getReasonType() { return reasonType; }
    public void setReasonType(String reasonType) { this.reasonType = reasonType; }
    public Long getViolationId() { return violationId; }
    public void setViolationId(Long violationId) { this.violationId = violationId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
}
