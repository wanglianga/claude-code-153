package com.waste.repository;

import com.waste.entity.RenovationRegistration;
import com.waste.enums.RegStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RenovationRegistrationRepository extends JpaRepository<RenovationRegistration, Long> {
    List<RenovationRegistration> findByOwnerIdOrderByIdDesc(Long ownerId);
    List<RenovationRegistration> findByStatus(RegStatus status);
    List<RenovationRegistration> findByBuilding(String building);
}
