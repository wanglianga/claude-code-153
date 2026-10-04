package com.waste.service;

import com.waste.enums.TraceStatus;
import com.waste.repository.BagTraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 袋级追溯状态推进器：状态只前进不后退（REPORTED→APPROVED→DISPATCHED→LOADED→WEIGHED→DISPOSED→SETTLED） */
@Component
@RequiredArgsConstructor
public class TraceUpdater {

    private final BagTraceRepository bagTraceRepo;

    public void advance(Long appointmentId, TraceStatus status) {
        bagTraceRepo.findByAppointmentId(appointmentId).ifPresent(t -> {
            if (status.ordinal() > t.getStatus().ordinal()) {
                t.setStatus(status);
                t.setUpdatedAt(LocalDateTime.now());
                bagTraceRepo.save(t);
            }
        });
    }
}
