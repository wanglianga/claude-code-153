package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.AnomalyStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

/** 称重数据异常记录 */
@Entity
@Table(name = "weigh_anomaly", indexes = @Index(name = "idx_anomaly_status", columnList = "status"))
public class WeighAnomaly extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String anomalyNo;

    private Long tripId;
    private Long appointmentId;
    private Long receiptId;

    /**
     * 异常类型：
     * NET_NEGATIVE 净重为负；DEVIATION 与预估偏差过大；
     * BAG_COUNT_MISMATCH 装车袋数与预约不符；RECEIPT_DIFF 处置场复称差异；
     * TARE_DRIFT 空车重量漂移
     */
    @Column(nullable = false, length = 24)
    private String type;

    @Column(nullable = false, length = 512)
    private String detail;

    @Column(precision = 10, scale = 2)
    private BigDecimal expectedValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AnomalyStatus status = AnomalyStatus.OPEN;

    private Long handledBy;
    private java.time.LocalDateTime handledAt;

    /** 认定的结算重量（调整后） */
    @Column(precision = 10, scale = 2)
    private BigDecimal resolvedWeight;

    @Column(length = 512)
    private String resolution;

    public String getAnomalyNo() { return anomalyNo; }
    public void setAnomalyNo(String anomalyNo) { this.anomalyNo = anomalyNo; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getReceiptId() { return receiptId; }
    public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getExpectedValue() { return expectedValue; }
    public void setExpectedValue(BigDecimal expectedValue) { this.expectedValue = expectedValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }
    public AnomalyStatus getStatus() { return status; }
    public void setStatus(AnomalyStatus status) { this.status = status; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public java.time.LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(java.time.LocalDateTime handledAt) { this.handledAt = handledAt; }
    public BigDecimal getResolvedWeight() { return resolvedWeight; }
    public void setResolvedWeight(BigDecimal resolvedWeight) { this.resolvedWeight = resolvedWeight; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
