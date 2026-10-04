package com.waste.entity;

import com.waste.enums.TraceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 袋级追溯：同一批袋装垃圾从预约到处置结算的全链路状态 */
@Data
@Entity
@Table(name = "bag_trace")
public class BagTrace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String traceCode;

    @Column(unique = true, nullable = false)
    private Long appointmentId;

    @Column(nullable = false)
    private Long registrationId;

    @Column(nullable = false)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TraceStatus status = TraceStatus.REPORTED;

    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime createdAt = LocalDateTime.now();
}
