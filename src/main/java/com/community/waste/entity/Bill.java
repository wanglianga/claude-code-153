package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.BillPartyType;
import com.community.waste.domain.DomainEnums.BillStatus;
import com.community.waste.domain.DomainEnums.BillType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单：财务端按 业主 / 施工队 / 物业垫付 / 清运公司 结算。
 * partyType + partyRef 标识应收/应付对象；owner/crew 以 registrationId 关联。
 */
@Entity
@Table(name = "bill", indexes = {
        @Index(name = "idx_bill_party", columnList = "partyType,partyRef"),
        @Index(name = "idx_bill_reg", columnList = "registration_id"),
        @Index(name = "idx_bill_status", columnList = "status")})
public class Bill extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String billNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillType billType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillPartyType partyType;

    /** 应收/应付对象标识：业主/施工队用 registrationId（字符串），公司用公司名，物业用 COMMUNITY */
    @Column(nullable = false, length = 64)
    private String partyRef;

    @Column(name = "registration_id")
    private Long registrationId;

    private Long tripId;
    private Long receiptId;
    private Long violationId;
    private Long complaintId;

    /** 重量 kg */
    @Column(precision = 10, scale = 2)
    private BigDecimal weightKg;

    /** 计费量（如标准袋数） */
    private Integer quantity;

    /** 单价 元/kg 或 元/袋 */
    @Column(precision = 10, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /** 应收方向：true=向对象收款，false=向对象付款（如付清运公司） */
    @Column(nullable = false)
    private Boolean receivable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BillStatus status = BillStatus.UNPAID;

    @Column(length = 255)
    private String summary;

    /** 押金抵扣金额 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositApplied = BigDecimal.ZERO;

    private LocalDateTime paidAt;
    @Column(length = 64)
    private String paidChannel;

    /** 争议后调整金额（负数表示减收） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal adjustment = BigDecimal.ZERO;

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public BillType getBillType() { return billType; }
    public void setBillType(BillType billType) { this.billType = billType; }
    public BillPartyType getPartyType() { return partyType; }
    public void setPartyType(BillPartyType partyType) { this.partyType = partyType; }
    public String getPartyRef() { return partyRef; }
    public void setPartyRef(String partyRef) { this.partyRef = partyRef; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getReceiptId() { return receiptId; }
    public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
    public Long getViolationId() { return violationId; }
    public void setViolationId(Long violationId) { this.violationId = violationId; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Boolean getReceivable() { return receivable; }
    public void setReceivable(Boolean receivable) { this.receivable = receivable; }
    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public BigDecimal getDepositApplied() { return depositApplied; }
    public void setDepositApplied(BigDecimal depositApplied) { this.depositApplied = depositApplied; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getPaidChannel() { return paidChannel; }
    public void setPaidChannel(String paidChannel) { this.paidChannel = paidChannel; }
    public BigDecimal getAdjustment() { return adjustment; }
    public void setAdjustment(BigDecimal adjustment) { this.adjustment = adjustment; }

    @com.fasterxml.jackson.annotation.JsonProperty("payableAmount")
    @Transient
    public BigDecimal payableAmount() {
        return amount.add(adjustment);
    }
}
