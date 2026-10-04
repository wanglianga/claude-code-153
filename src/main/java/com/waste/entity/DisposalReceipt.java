package com.waste.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 处置场回执 */
@Data
@Entity
@Table(name = "disposal_receipt")
public class DisposalReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long weighRecordId;

    @Column(nullable = false)
    private Long appointmentId;

    @Column(nullable = false, length = 64)
    private String disposalSite;    // 处置场

    @Column(unique = true, nullable = false, length = 32)
    private String receiptNo;       // 回执单号

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal receivedWeightKg; // 处置场接收重量

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
