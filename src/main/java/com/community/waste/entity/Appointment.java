package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.AppointmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 清运预约（物业审核日报后生成） */
@Entity
@Table(name = "appointment",
        indexes = {@Index(name = "idx_appt_point_time", columnList = "point_id,scheduledTime"),
                @Index(name = "idx_appt_status", columnList = "status")})
public class Appointment extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String apptNo;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    /** 预约袋数 */
    @Column(nullable = false)
    private Integer bagCount;

    /** 预估总重 kg */
    @Column(nullable = false)
    private java.math.BigDecimal estimatedKg = java.math.BigDecimal.ZERO;

    /** 计划清运时间 */
    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    /** 最晚清运时间（超过视为超时未清/迟到） */
    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private AppointmentStatus status = AppointmentStatus.APPROVED;

    private Long createdBy;
    private Long tripId;

    /** 车辆迟到分钟数 */
    private Integer lateMinutes;

    /** 是否发生垃圾点溢出 */
    @Column(nullable = false)
    private Boolean spillOccurred = false;

    @Column(length = 255)
    private String remark;

    public String getApptNo() { return apptNo; }
    public void setApptNo(String apptNo) { this.apptNo = apptNo; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getPointId() { return pointId; }
    public void setPointId(Long pointId) { this.pointId = pointId; }
    public Integer getBagCount() { return bagCount; }
    public void setBagCount(Integer bagCount) { this.bagCount = bagCount; }
    public java.math.BigDecimal getEstimatedKg() { return estimatedKg; }
    public void setEstimatedKg(java.math.BigDecimal estimatedKg) { this.estimatedKg = estimatedKg; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public Boolean getSpillOccurred() { return spillOccurred; }
    public void setSpillOccurred(Boolean spillOccurred) { this.spillOccurred = spillOccurred; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
