package com.waste.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 全链路追溯事件：谁、在什么时间、对哪个单据、做了什么 */
@Data
@Entity
@Table(name = "trace_event", indexes = {
        @Index(name = "idx_trace_appt", columnList = "appointmentId"),
        @Index(name = "idx_trace_entity", columnList = "entityType,entityId")
})
public class TraceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appointmentId;     // 冗余预约 ID 便于全链路查询（可空）

    @Column(nullable = false, length = 32)
    private String entityType;      // REGISTRATION/REPORT/APPOINTMENT/DISPATCH/WEIGH/DISPOSAL/BILL/VIOLATION/COMPLAINT/WASTE_POINT

    @Column(nullable = false)
    private Long entityId;

    private Long actorId;           // 操作人（系统事件为空）

    @Column(length = 64)
    private String actorName;

    @Column(length = 16)
    private String actorRole;

    @Column(nullable = false, length = 64)
    private String action;          // 动作，如 SUBMIT_REPORT / REVIEW_APPROVE / WEIGH_IN_OUT

    @Column(length = 512)
    private String detail;

    private LocalDateTime createdAt = LocalDateTime.now();
}
