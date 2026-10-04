package com.community.waste.repository;

import com.community.waste.entity.DisposalReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisposalReceiptRepository extends JpaRepository<DisposalReceipt, Long> {
    Optional<DisposalReceipt> findByTripId(Long tripId);
    Optional<DisposalReceipt> findByReceiptNo(String receiptNo);
}
