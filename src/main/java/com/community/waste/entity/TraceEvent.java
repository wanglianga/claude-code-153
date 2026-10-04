package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.TraceType;
import jakarta.persistence.*;

/**
 * 全链路追溯事件。任何一袋垃圾的关键节点都落一条事件，
 * 投诉时可按 bagCode / 房号还原：谁堆放、谁审核、谁清走、费用如何产生。
 */
@Entity
@Table(name = "trace_event", indexes = {
        @Index(name = "idx_trace_bag", columnList = "bagCode"),
        @Index(name = "idx_trace_reg", columnList = "registration_id"),
        @Index(name = "idx_trace_appt", columnList = "appointmentId"),
        @Index(name = "idx_trace_trip", columnList = "tripId"),
        @Index(name = "idx_trace_type", columnList = "type")})
public class TraceEvent extends BaseEntity {
    /** 关联垃圾袋编码，可空（如装修登记事件先于袋产生） */
    @Column(length = 48)
    private String bagCode;

    @Column(name = "registration_id")
    private Long registrationId;

    private Long appointmentId;
    private Long tripId;
    private Long billId;
    private Long violationId;
    private Long complaintId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TraceType type;

    /** 操作角色：OWNER/CREW/PROPERTY/COMPANY/SYSTEM/FINANCE */
    @Column(length = 16)
    private String actorRole;

    @Column(length = 64)
    private String actorName;

    @Column(nullable = false, length = 1024)
    private String detail;

    public TraceEvent() {}

    public TraceEvent(TraceType type, String detail) {
        this.type = type;
        this.detail = detail;
    }

    public String getBagCode() { return bagCode; }
    public void setBagCode(String bagCode) { this.bagCode = bagCode; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getViolationId() { return violationId; }
    public void setViolationId(Long violationId) { this.violationId = violationId; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public TraceType getType() { return type; }
    public void setType(TraceType type) { this.type = type; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
