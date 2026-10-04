package com.community.waste.repository;

import com.community.waste.entity.Violation;
import com.community.waste.domain.DomainEnums.ViolationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    List<Violation> findByRegistrationId(Long registrationId);
    List<Violation> findByStatus(ViolationStatus status);
    List<Violation> findByComplaintId(Long complaintId);
}
