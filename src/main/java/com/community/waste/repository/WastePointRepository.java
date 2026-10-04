package com.community.waste.repository;

import com.community.waste.entity.WastePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WastePointRepository extends JpaRepository<WastePoint, Long> {
    Optional<WastePoint> findByCode(String code);
}
