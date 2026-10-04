package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.BagStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 单袋（或同批次按序号管理的一袋）装修垃圾。
 * 同一袋垃圾从上报、验收、装车、称重到处置回执全程可追踪。
 */
@Entity
@Table(name = "waste_bag",
        indexes = {
                @Index(name = "idx_bag_reg", columnList = "registration_id"),
                @Index(name = "idx_bag_status", columnList = "status")
        })
public class WasteBag extends BaseEntity {
    /** 袋编码：R{registrationId}-D{reportId}-{seq}，现场贴码 */
    @Column(nullable = false, unique = true, length = 48)
    private String bagCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id")
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_report_id")
    private DailyReport dailyReport;

    /** 序号（该日报内从1开始） */
    @Column(nullable = false)
    private Integer seqNo;

    /** 是否临时加袋 */
    @Column(nullable = false)
    private Boolean extra = false;

    /** 分类标签 wood/glass/ceramic/mixed */
    @Column(length = 32)
    private String category;

    /** 预估重量（kg），验收时可修正 */
    @Column(precision = 8, scale = 2)
    private BigDecimal estimatedKg;

    /** 实际分摊重量（kg），按车次净重按袋分摊 */
    @Column(precision = 8, scale = 2)
    private BigDecimal actualKg;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private BagStatus status = BagStatus.REPORTED;

    private Long appointmentId;
    private Long tripId;
    private Long acceptedBy;
    private java.time.LocalDateTime acceptedAt;
    private java.time.LocalDateTime loadedAt;
    private java.time.LocalDateTime disposedAt;

    public String getBagCode() { return bagCode; }
    public void setBagCode(String bagCode) { this.bagCode = bagCode; }
    public Registration getRegistration() { return registration; }
    public void setRegistration(Registration registration) { this.registration = registration; }
    public DailyReport getDailyReport() { return dailyReport; }
    public void setDailyReport(DailyReport dailyReport) { this.dailyReport = dailyReport; }
    public Integer getSeqNo() { return seqNo; }
    public void setSeqNo(Integer seqNo) { this.seqNo = seqNo; }
    public Boolean getExtra() { return extra; }
    public void setExtra(Boolean extra) { this.extra = extra; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getEstimatedKg() { return estimatedKg; }
    public void setEstimatedKg(BigDecimal estimatedKg) { this.estimatedKg = estimatedKg; }
    public BigDecimal getActualKg() { return actualKg; }
    public void setActualKg(BigDecimal actualKg) { this.actualKg = actualKg; }
    public BagStatus getStatus() { return status; }
    public void setStatus(BagStatus status) { this.status = status; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(Long acceptedBy) { this.acceptedBy = acceptedBy; }
    public java.time.LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(java.time.LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public java.time.LocalDateTime getLoadedAt() { return loadedAt; }
    public void setLoadedAt(java.time.LocalDateTime loadedAt) { this.loadedAt = loadedAt; }
    public java.time.LocalDateTime getDisposedAt() { return disposedAt; }
    public void setDisposedAt(java.time.LocalDateTime disposedAt) { this.disposedAt = disposedAt; }
}
