package com.waste.entity;

import com.waste.enums.Frequency;
import com.waste.enums.RegStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 装修登记：业主装修前在物业处登记 */
@Data
@Entity
@Table(name = "renovation_registration")
public class RenovationRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String regNo;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 64)
    private String ownerName;

    @Column(nullable = false, length = 32)
    private String building;        // 楼栋

    @Column(nullable = false, length = 32)
    private String roomNumber;      // 房号

    @Column(nullable = false, length = 64)
    private String constructionTeam; // 施工队

    @Column(nullable = false)
    private LocalDate expectedStartDate;

    @Column(nullable = false)
    private LocalDate expectedEndDate;

    /** 垃圾类型，逗号分隔：木板,玻璃,瓷砖,混凝土,其他 */
    @Column(nullable = false, length = 128)
    private String wasteTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Frequency frequency;    // 清运频次

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;   // 押金总额

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositBalance;  // 押金余额

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RegStatus status = RegStatus.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();
}
