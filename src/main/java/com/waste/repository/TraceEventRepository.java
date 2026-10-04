package com.waste.repository;

import com.waste.entity.TraceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceEventRepository extends JpaRepository<TraceEvent, Long> {
    List<TraceEvent> findByAppointmentIdOrderByIdAsc(Long appointmentId);
    List<TraceEvent> findByEntityTypeAndEntityIdOrderByIdAsc(String entityType, Long entityId);
}
