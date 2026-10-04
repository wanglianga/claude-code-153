package com.waste.repository;

import com.waste.entity.DailyReport;
import com.waste.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    List<DailyReport> findByRegistrationIdOrderByIdDesc(Long registrationId);
    List<DailyReport> findByStatusOrderByIdDesc(ReportStatus status);
    List<DailyReport> findByRegistrationIdAndReportDate(Long registrationId, LocalDate reportDate);
    Optional<DailyReport> findTopByRegistrationIdOrderByReportDateDesc(Long registrationId);
}
