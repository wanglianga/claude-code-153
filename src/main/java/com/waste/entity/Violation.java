package com.waste.entity;

import com.waste.enums.ViolationStatus;
import com.waste.enums.ViolationType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 违规记录（消防通道堆放/混入生活垃圾/溢出） */
@Data
@Entity
@Table(name = "violation")
public class Violation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String violationNo;

    @Column(nullable = false)
    private Long registrationId;

    private Long reportId;
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ViolationType type;

    @Column(nullable = false, length = 256)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionAmount; // 拟扣款金额

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ViolationStatus status = ViolationStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
}
