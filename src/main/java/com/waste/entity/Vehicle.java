package com.waste.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/** 清运车辆 */
@Data
@Entity
@Table(name = "vehicle")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 16)
    private String plateNo;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal capacityTons;    // 核载吨位

    @Column(nullable = false, length = 64)
    private String driverName;

    @Column(length = 32)
    private String driverPhone;

    @Column(nullable = false, length = 64)
    private String companyName;     // 所属清运公司
}
