package com.waste.repository;

import com.waste.entity.Violation;
import com.waste.enums.ViolationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    List<Violation> findByRegistrationIdOrderByIdDesc(Long registrationId);
    List<Violation> findByStatusOrderByIdDesc(ViolationStatus status);
    List<Violation> findByAppointmentId(Long appointmentId);
}
