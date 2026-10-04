package com.community.waste.repository;

import com.community.waste.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByRegistrationId(Long registrationId);
    List<Complaint> findByStatus(com.community.waste.domain.DomainEnums.ComplaintStatus status);
}
