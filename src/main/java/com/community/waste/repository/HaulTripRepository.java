package com.community.waste.repository;

import com.community.waste.entity.HaulTrip;
import com.community.waste.domain.DomainEnums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HaulTripRepository extends JpaRepository<HaulTrip, Long> {
    Optional<HaulTrip> findByTripNo(String tripNo);
    Optional<HaulTrip> findByManifestNo(String manifestNo);
    List<HaulTrip> findByStatusIn(List<TripStatus> statuses);
    List<HaulTrip> findByPlateAndStatusIn(String plate, List<TripStatus> statuses);
}
