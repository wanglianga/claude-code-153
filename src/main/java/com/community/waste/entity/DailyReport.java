package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.ReportStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

/** 施工队每日堆放上报 */
@Entity
@Table(name = "daily_report",
        indexes = @Index(name = "idx_report_reg_date", columnList = "registration_id,reportDate", unique = true))
public class DailyReport extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id")
    private Registration registration;

    @Column(nullable = false)
    private LocalDate reportDate;

    /** 袋装数量 */
    @Column(nullable = false)
    private Integer bagCount;

    /** 堆放位置（点位编码或文字描述） */
    @Column(nullable = false, length = 128)
    private String pileLocation;

    @Column(name = "point_id")
    private Long pointId;

    private Boolean containsWood = false;
    private Boolean containsGlass = false;
    private Boolean containsCeramic = false;
    private Boolean containsHousehold = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ReportStatus status = ReportStatus.SUBMITTED;

    private Long auditedBy;
    private java.time.LocalDateTime auditedAt;

    @Column(length = 255)
    private String auditRemark;

    public Registration getRegistration() { return registration; }
    public void setRegistration(Registration registration) { this.registration = registration; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public Integer getBagCount() { return bagCount; }
    public void setBagCount(Integer bagCount) { this.bagCount = bagCount; }
    public String getPileLocation() { return pileLocation; }
    public void setPileLocation(String pileLocation) { this.pileLocation = pileLocation; }
    public Long getPointId() { return pointId; }
    public void setPointId(Long pointId) { this.pointId = pointId; }
    public Boolean getContainsWood() { return containsWood; }
    public void setContainsWood(Boolean containsWood) { this.containsWood = containsWood; }
    public Boolean getContainsGlass() { return containsGlass; }
    public void setContainsGlass(Boolean containsGlass) { this.containsGlass = containsGlass; }
    public Boolean getContainsCeramic() { return containsCeramic; }
    public void setContainsCeramic(Boolean containsCeramic) { this.containsCeramic = containsCeramic; }
    public Boolean getContainsHousehold() { return containsHousehold; }
    public void setContainsHousehold(Boolean containsHousehold) { this.containsHousehold = containsHousehold; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public Long getAuditedBy() { return auditedBy; }
    public void setAuditedBy(Long auditedBy) { this.auditedBy = auditedBy; }
    public java.time.LocalDateTime getAuditedAt() { return auditedAt; }
    public void setAuditedAt(java.time.LocalDateTime auditedAt) { this.auditedAt = auditedAt; }
    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }
}
