package com.waste.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 称重记录：车辆进出场称重 + 现场照片 + 清运联单 */
@Data
@Entity
@Table(name = "weigh_record")
public class WeighRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dispatchId;

    @Column(nullable = false)
    private Long appointmentId;

    @Column(nullable = false, length = 16)
    private String plateNo;

    @Column(nullable = false, length = 64)
    private String driverName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal emptyWeightKg;   // 空车重量

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal loadedWeightKg;  // 装载重量

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netWeightKg;     // 净重 = 装载 - 空车

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedWeightKg; // 按袋数预估重量

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal deviationPct;    // 偏差百分比

    @Column(nullable = false)
    private Boolean anomalyFlag = false; // 称重数据异常

    @Column(length = 256)
    private String anomalyReason;

    @Column(nullable = false)
    private LocalDateTime entryTime;    // 进场时间

    @Column(nullable = false)
    private LocalDateTime exitTime;     // 出场时间

    /** 现场照片，逗号分隔 URL */
    @Column(length = 1024)
    private String photos;

    /** 清运联单号 */
    @Column(unique = true, nullable = false, length = 32)
    private String manifestNo;

    private LocalDateTime createdAt = LocalDateTime.now();
}
