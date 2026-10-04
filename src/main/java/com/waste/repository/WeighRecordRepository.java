package com.waste.repository;

import com.waste.entity.WeighRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeighRecordRepository extends JpaRepository<WeighRecord, Long> {
    Optional<WeighRecord> findByDispatchId(Long dispatchId);
    List<WeighRecord> findByAppointmentId(Long appointmentId);
    List<WeighRecord> findByAnomalyFlagTrue();
}
