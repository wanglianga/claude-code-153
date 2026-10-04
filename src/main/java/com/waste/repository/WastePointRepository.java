package com.waste.repository;

import com.waste.entity.WastePoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WastePointRepository extends JpaRepository<WastePoint, Long> {
}
