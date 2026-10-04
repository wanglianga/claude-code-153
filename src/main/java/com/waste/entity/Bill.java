package com.waste.entity;

import com.waste.enums.BillStatus;
import com.waste.enums.BillType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账单：业主 / 施工队 / 物业垫付 / 清运公司结算 */
@Data
@Entity
@Table(name = "bill")
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String billNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillType billType;

    private Long partyId;           // 结算对象用户 ID（可空，如施工队/物业按名称）

    @Column(nullable = false, length = 64)
    private String partyName;       // 结算对象名称

    private Long registrationId;
    private Long appointmentId;

    @Column(nullable = false, length = 8)
    private String period;          // 账期 yyyy-MM

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BillStatus status = BillStatus.UNPAID;

    /** 费用明细说明（JSON 文本） */
    @Column(length = 1024)
    private String detail;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime paidAt;
}
