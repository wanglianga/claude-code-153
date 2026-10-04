package com.community.waste.repository;

import com.community.waste.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByCommunityCodeAndRoomNo(String communityCode, String roomNo);
    List<Registration> findByBuildingNo(String buildingNo);
    List<Registration> findByStatus(com.community.waste.domain.DomainEnums.RegistrationStatus status);
}
