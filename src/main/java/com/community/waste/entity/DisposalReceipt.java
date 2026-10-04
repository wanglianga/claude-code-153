package com.community.waste.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 处置场回执（车辆到达处置场后的消纳凭证） */
@Entity
@Table(name = "disposal_receipt",
        uniqueConstraints = @UniqueConstraint(name = "uk_receipt_trip", columnNames = "trip_id"))
public class DisposalReceipt extends BaseEntity {
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false, length = 40)
    private String receiptNo;

    @Column(nullable = false, length = 64)
    private String siteName;

    /** 处置场复称重量 kg */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal receivedWeight;

    /** 处置单价 元/kg */
    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal unitPrice;

    /** 处置费金额 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal disposalFee;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(length = 255)
    private String operator;

    @Column(length = 512)
    private String photoUrl;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public BigDecimal getReceivedWeight() { return receivedWeight; }
    public void setReceivedWeight(BigDecimal receivedWeight) { this.receivedWeight = receivedWeight; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDisposalFee() { return disposalFee; }
    public void setDisposalFee(BigDecimal disposalFee) { this.disposalFee = disposalFee; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
