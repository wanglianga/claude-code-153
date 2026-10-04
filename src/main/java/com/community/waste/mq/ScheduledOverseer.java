package com.community.waste.mq;

import com.community.waste.domain.DomainEnums.AppointmentStatus;
import com.community.waste.entity.Appointment;
import com.community.waste.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时巡检：已派单/已到场但超过最晚清运时限仍未装车离场的预约，
 * 判定车辆迟到（可能导致垃圾点溢出），发布 TRIP_LATE 事件。
 * lateMinutes 非空表示已发布过，避免重复。
 */
@Component
public class ScheduledOverseer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledOverseer.class);

    private final AppointmentRepository appointmentRepository;
    private final EventPublisher publisher;

    public ScheduledOverseer(AppointmentRepository appointmentRepository, EventPublisher publisher) {
        this.appointmentRepository = appointmentRepository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${app.overseer.delay-ms:60000}")
    @Transactional
    public void checkOverdueAppointments() {
        List<AppointmentStatus> active = List.of(
                AppointmentStatus.APPROVED, AppointmentStatus.DISPATCHED, AppointmentStatus.ARRIVED);
        List<Appointment> overdue = appointmentRepository
                .findByScheduledTimeBeforeAndStatusIn(LocalDateTime.now(), active);
        for (Appointment a : overdue) {
            if (a.getDeadline() == null || a.getLateMinutes() != null) continue;
            if (LocalDateTime.now().isAfter(a.getDeadline())) {
                int minutes = (int) Duration.between(a.getDeadline(), LocalDateTime.now()).toMinutes();
                a.setLateMinutes(minutes);
                appointmentRepository.save(a);
                log.warn("预约 {} 超过最晚清运时限 {} 分钟，发布车辆迟到事件", a.getApptNo(), minutes);
                publisher.publish("waste.trip.late",
                        DomainEvent.of("TRIP_LATE", "预约 " + a.getApptNo() + " 超时未清 " + minutes + " 分钟")
                                .ref(a.getRegistrationId(), a.getId(), a.getTripId())
                                .put("lateMinutes", minutes)
                                .put("appointmentIds", String.valueOf(a.getId())));
            }
        }
    }
}
