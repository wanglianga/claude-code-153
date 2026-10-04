package com.waste.entity;

import com.waste.enums.DispatchStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 清运派单：清运公司按门禁/限行/容量/已有派单安排车辆 */
@Data
@Entity
@Table(name = "dispatch_order")
public class DispatchOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String dispatchNo;

    @Column(nullable = false)
    private Long appointmentId;

    @Column(nullable = false)
    private Long vehicleId;

    @Column(nullable = false, length = 16)
    private String plateNo;

    @Column(nullable = false, length = 64)
    private String driverName;

    @Column(nullable = false, length = 64)
    private String companyName;

    /** 小区门禁放行单号 */
    @Column(nullable = false, length = 32)
    private String gatePassNo;

    @Column(nullable = false)
    private LocalDateTime plannedArrival;

    private LocalDateTime actualArrival;

    @Column(nullable = false)
    private Boolean lateFlag = false;

    @Column(length = 256)
    private String routeInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DispatchStatus status = DispatchStatus.DISPATCHED;

    private LocalDateTime createdAt = LocalDateTime.now();
}
