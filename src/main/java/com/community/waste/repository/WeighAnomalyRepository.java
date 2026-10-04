package com.community.waste.repository;

import com.community.waste.entity.WeighAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeighAnomalyRepository extends JpaRepository<WeighAnomaly, Long> {
    List<WeighAnomaly> findByTripId(Long tripId);
    List<WeighAnomaly> findByStatus(com.community.waste.domain.DomainEnums.AnomalyStatus status);
}
