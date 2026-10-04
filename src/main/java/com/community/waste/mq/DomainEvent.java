package com.community.waste.mq;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** 领域事件（投递到 waste.events） */
public class DomainEvent {
    private String eventType;
    private Long registrationId;
    private Long appointmentId;
    private Long tripId;
    private Long violationId;
    private Long complaintId;
    private Long billId;
    private String bagCode;
    private String detail;
    private LocalDateTime occurredAt = LocalDateTime.now();
    private Map<String, Object> extra = new LinkedHashMap<>();

    public DomainEvent() {}

    public DomainEvent(String eventType, String detail) {
        this.eventType = eventType;
        this.detail = detail;
    }

    public static DomainEvent of(String type, String detail) {
        return new DomainEvent(type, detail);
    }

    public DomainEvent ref(Long registrationId, Long appointmentId, Long tripId) {
        this.registrationId = registrationId;
        this.appointmentId = appointmentId;
        this.tripId = tripId;
        return this;
    }

    public DomainEvent put(String key, Object value) {
        this.extra.put(key, value);
        return this;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getViolationId() { return violationId; }
    public void setViolationId(Long violationId) { this.violationId = violationId; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getBagCode() { return bagCode; }
    public void setBagCode(String bagCode) { this.bagCode = bagCode; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
