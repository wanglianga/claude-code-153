package com.community.waste.repository;

import com.community.waste.domain.DomainEnums.BagStatus;
import com.community.waste.entity.WasteBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WasteBagRepository extends JpaRepository<WasteBag, Long> {
    Optional<WasteBag> findByBagCode(String bagCode);
    List<WasteBag> findByRegistrationIdOrderByCreatedAtAsc(Long registrationId);
    List<WasteBag> findByDailyReportId(Long reportId);
    List<WasteBag> findByStatus(BagStatus status);
    long countByDailyReportIdAndStatus(Long reportId, BagStatus status);

    @Query("select count(b) from WasteBag b where b.dailyReport.pointId = :pointId and b.status in :statuses")
    long countOccupyingAtPoint(@Param("pointId") Long pointId, @Param("statuses") Collection<BagStatus> statuses);
}
