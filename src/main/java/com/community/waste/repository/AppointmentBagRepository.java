package com.community.waste.repository;

import com.community.waste.entity.AppointmentBag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentBagRepository extends JpaRepository<AppointmentBag, Long> {
    List<AppointmentBag> findByAppointmentId(Long appointmentId);
    List<AppointmentBag> findByBagId(Long bagId);
    long countByAppointmentId(Long appointmentId);
}
