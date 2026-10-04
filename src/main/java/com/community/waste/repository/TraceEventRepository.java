package com.community.waste.repository;

import com.community.waste.entity.TraceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceEventRepository extends JpaRepository<TraceEvent, Long> {
    List<TraceEvent> findByRegistrationIdOrderByCreatedAtAsc(Long registrationId);
    List<TraceEvent> findByBagCodeOrderByCreatedAtAsc(String bagCode);
    List<TraceEvent> findByAppointmentIdOrderByCreatedAtAsc(Long appointmentId);
    List<TraceEvent> findByTripIdOrderByCreatedAtAsc(Long tripId);
}
