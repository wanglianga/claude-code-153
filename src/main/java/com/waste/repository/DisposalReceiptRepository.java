package com.waste.repository;

import com.waste.entity.DisposalReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisposalReceiptRepository extends JpaRepository<DisposalReceipt, Long> {
    Optional<DisposalReceipt> findByWeighRecordId(Long weighRecordId);
    Optional<DisposalReceipt> findByAppointmentId(Long appointmentId);
}
