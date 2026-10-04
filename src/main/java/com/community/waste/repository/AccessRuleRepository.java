package com.community.waste.repository;

import com.community.waste.entity.AccessRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRuleRepository extends JpaRepository<AccessRule, Long> {
    List<AccessRule> findByCommunityCodeAndEnabledTrue(String communityCode);
}
