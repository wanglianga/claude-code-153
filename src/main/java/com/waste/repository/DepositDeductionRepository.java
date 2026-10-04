package com.waste.repository;

import com.waste.entity.DepositDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositDeductionRepository extends JpaRepository<DepositDeduction, Long> {
    List<DepositDeduction> findByRegistrationIdOrderByIdDesc(Long registrationId);
}
