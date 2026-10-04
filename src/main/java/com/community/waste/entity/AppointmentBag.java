package com.community.waste.entity;

import jakarta.persistence.*;

/** 预约-垃圾袋关联（一次预约包含哪些袋） */
@Entity
@Table(name = "appointment_bag",
        uniqueConstraints = @UniqueConstraint(name = "uk_appt_bag", columnNames = {"appointment_id", "bag_id"}))
public class AppointmentBag extends BaseEntity {
    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "bag_id", nullable = false)
    private Long bagId;

    public AppointmentBag() {}
    public AppointmentBag(Long appointmentId, Long bagId) {
        this.appointmentId = appointmentId;
        this.bagId = bagId;
    }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getBagId() { return bagId; }
    public void setBagId(Long bagId) { this.bagId = bagId; }
}
