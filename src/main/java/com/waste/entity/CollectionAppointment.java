package com.waste.entity;

import com.waste.enums.ApptStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 清运预约：物业审核每日上报后生成 */
@Data
@Entity
@Table(name = "collection_appointment")
public class CollectionAppointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String apptNo;

    @Column(nullable = false)
    private Long registrationId;

    @Column(nullable = false)
    private Long reportId;

    @Column(nullable = false)
    private Long wastePointId;

    @Column(nullable = false)
    private LocalDate apptDate;

    @Column(nullable = false, length = 8)
    private String timeSlot;    // AM / PM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ApptStatus status;

    /** 容量校验说明（容量不足时给出错峰/围挡提示） */
    @Column(length = 256)
    private String capacityMessage;

    private Long reviewerId;

    @Column(length = 64)
    private String reviewerName;

    @Column(length = 256)
    private String reviewComment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
