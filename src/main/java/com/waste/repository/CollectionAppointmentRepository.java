package com.waste.repository;

import com.waste.entity.CollectionAppointment;
import com.waste.enums.ApptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CollectionAppointmentRepository extends JpaRepository<CollectionAppointment, Long> {
    List<CollectionAppointment> findByRegistrationIdOrderByIdDesc(Long registrationId);
    List<CollectionAppointment> findByStatusOrderByIdDesc(ApptStatus status);
    List<CollectionAppointment> findByStatusInOrderByIdDesc(List<ApptStatus> statuses);
    List<CollectionAppointment> findByApptDateBeforeAndStatusIn(LocalDate date, List<ApptStatus> statuses);
    List<CollectionAppointment> findByReportId(Long reportId);
}
