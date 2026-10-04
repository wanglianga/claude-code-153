package com.community.waste.repository;

import com.community.waste.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    Optional<DailyReport> findByRegistrationIdAndReportDate(Long registrationId, LocalDate reportDate);
    List<DailyReport> findByRegistrationIdOrderByReportDateDesc(Long registrationId);
    List<DailyReport> findByStatus(com.community.waste.domain.DomainEnums.ReportStatus status);
}
