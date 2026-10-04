package com.community.waste.repository;

import com.community.waste.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByBillId(Long billId);
    List<Dispute> findByStatus(com.community.waste.domain.DomainEnums.DisputeStatus status);
}
