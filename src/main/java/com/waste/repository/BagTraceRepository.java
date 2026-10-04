package com.waste.repository;

import com.waste.entity.BagTrace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BagTraceRepository extends JpaRepository<BagTrace, Long> {
    Optional<BagTrace> findByAppointmentId(Long appointmentId);
    Optional<BagTrace> findByTraceCode(String traceCode);
}
