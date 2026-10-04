package com.community.waste.repository;

import com.community.waste.entity.Appointment;
import com.community.waste.domain.DomainEnums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPointIdAndStatusIn(Long pointId, List<AppointmentStatus> statuses);
    List<Appointment> findByStatusIn(List<AppointmentStatus> statuses);
    List<Appointment> findByRegistrationId(Long registrationId);
    List<Appointment> findByScheduledTimeBeforeAndStatusIn(LocalDateTime time, List<AppointmentStatus> statuses);
    long countByPointIdAndStatus(Long pointId, AppointmentStatus status);
}
