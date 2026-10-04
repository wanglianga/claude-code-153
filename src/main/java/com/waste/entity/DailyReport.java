package com.waste.entity;

import com.waste.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 施工队每日袋装上报 */
@Data
@Entity
@Table(name = "daily_report")
public class DailyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long registrationId;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false)
    private Integer bagCount;       // 袋装数量（含临时加袋）

    @Column(nullable = false)
    private Integer extraBagCount = 0;  // 其中业主临时加袋数量

    @Column(nullable = false, length = 128)
    private String stackingLocation;    // 堆放位置

    @Column(nullable = false)
    private Long wastePointId;          // 所属垃圾点

    private Boolean containsWood = false;   // 含木板
    private Boolean containsGlass = false;  // 含玻璃
    private Boolean containsTile = false;   // 含瓷砖
    private Boolean containsMixedHousehold = false; // 混入生活垃圾

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(nullable = false)
    private Long submitterId;

    @Column(nullable = false, length = 64)
    private String submitterName;

    @Column(length = 256)
    private String reviewComment;

    private LocalDateTime createdAt = LocalDateTime.now();
}
