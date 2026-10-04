package com.community.waste.repository;

import com.community.waste.entity.FeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeConfigRepository extends JpaRepository<FeeConfig, Long> {
    Optional<FeeConfig> findByCommunityCodeAndParamKey(String communityCode, String paramKey);
}
