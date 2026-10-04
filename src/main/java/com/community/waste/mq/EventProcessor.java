package com.community.waste.mq;

import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.Appointment;
import com.community.waste.entity.WastePoint;
import com.community.waste.repository.AppointmentRepository;
import com.community.waste.repository.WastePointRepository;
import com.community.waste.service.CapacityService;
import com.community.waste.service.FeeService;
import com.community.waste.service.ViolationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 事件处理器：MQ 在线时由队列驱动；离线时由 EventPublisher 同步调用 handle()，行为一致。
 */
@Component
public class EventProcessor {

    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

    private final ViolationService violationService;
    private final AppointmentRepository appointmentRepository;
    private final WastePointRepository pointRepository;
    private final CapacityService capacityService;
    private final FeeService feeService;

    public EventProcessor(ViolationService violationService,
                          AppointmentRepository appointmentRepository,
                          WastePointRepository pointRepository,
                          CapacityService capacityService,
                          FeeService feeService) {
        this.violationService = violationService;
        this.appointmentRepository = appointmentRepository;
        this.pointRepository = pointRepository;
        this.capacityService = capacityService;
        this.feeService = feeService;
    }

    @RabbitListener(queues = MqConfig.Q_APPOINTMENT)
    public void onAppointment(DomainEvent event) {
        handle(event);
    }

    @RabbitListener(queues = MqConfig.Q_VIOLATION)
    public void onViolation(DomainEvent event) {
        handle(event);
    }

    @RabbitListener(queues = MqConfig.Q_WEIGH)
    public void onWeigh(DomainEvent event) {
        handle(event);
    }

    @RabbitListener(queues = MqConfig.Q_COMPLAINT)
    public void onComplaint(DomainEvent event) {
        handle(event);
    }

    @Transactional
    public void handle(DomainEvent event) {
        if (event == null || event.getEventType() == null) return;
        log.info("处理领域事件 {}: {}", event.getEventType(), event.getDetail());
        switch (event.getEventType()) {
            case "TRIP_LATE" -> handleTripLate(event);
            case "VIOLATION_OPENED", "COMPLAINT_SUBMITTED", "APPOINTMENT_CREATED",
                 "DISPATCHED", "VEHICLE_ENTER", "WEIGH_ANOMALY", "WEIGH_DONE",
                 "COMPLAINT_RESOLVED", "DISPOSAL_RECEIVED", "EXTRA_BAGS", "DISPUTE_RAISED" ->
                    log.debug("事件 {} 已记录留痕", event.getEventType());
            default -> log.debug("未细分处理事件：{}", event.getEventType());
        }
    }

    /** 车辆迟到 → 认定清运公司履约违约；点位已满则标记垃圾点溢出 */
    private void handleTripLate(DomainEvent event) {
        String ids = (String) event.getExtra().get("appointmentIds");
        if (ids == null) return;
        List<Long> apptIds = Arrays.stream(ids.split(",")).map(String::trim)
                .filter(s -> !s.isBlank()).map(Long::valueOf).toList();
        Object lateObj = event.getExtra().get("lateMinutes");
        int lateMinutes = lateObj instanceof Number n ? n.intValue() : 0;

        for (Long apptId : apptIds) {
            Appointment a = appointmentRepository.findById(apptId).orElse(null);
            if (a == null) continue;
            WastePoint point = pointRepository.findById(a.getPointId()).orElse(null);
            boolean spill = false;
            if (point != null) {
                capacityService.refreshPointStatus(point.getId());
                point = pointRepository.findById(point.getId()).orElse(point);
                spill = point.getStatus() == PointStatus.FULL || point.getStatus() == PointStatus.NEAR_FULL;
            }
            if (spill) {
                a.setSpillOccurred(true);
                appointmentRepository.save(a);
            }
            BigDecimal fine = feeService.get(point == null ? "DEFAULT" : point.getCommunityCode(),
                    spill ? FeeService.FINE_SPILL : FeeService.FINE_LATE,
                    BigDecimal.valueOf(spill ? 800 : 200));
            violationService.open(spill ? ViolationType.SPILL_OVERDUE : ViolationType.OTHER,
                    ViolationSource.SYSTEM,
                    "清运车辆迟到 " + lateMinutes + " 分钟" + (spill ? "，垃圾点已接近/达到容量上限发生溢出" : ""),
                    a.getRegistrationId(), a.getId(), event.getTripId(), null,
                    "COMPANY", fine, "系统派单时限巡检", true);
        }
    }
}
