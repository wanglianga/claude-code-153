package com.waste.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 押金流水（扣款/退款） */
@Data
@Entity
@Table(name = "deposit_deduction")
public class DepositDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long registrationId;

    private Long violationId;   // 关联违规（可空）
    private Long billId;        // 关联账单（可空）

    /** FEE 清运费抵扣 / FINE 违规罚款 / REFUND 退款返还 */
    @Column(nullable = false, length = 16)
    private String type;

    /** 正数为扣款，负数为返还 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;    // 操作后押金余额

    private LocalDateTime createdAt = LocalDateTime.now();
}
