package com.community.waste.repository;

import com.community.waste.entity.DepositDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositDeductionRepository extends JpaRepository<DepositDeduction, Long> {
    List<DepositDeduction> findByRegistrationIdOrderByCreatedAtAsc(Long registrationId);
}
