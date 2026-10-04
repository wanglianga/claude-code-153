package com.waste.common;

import com.waste.auth.Session;
import com.waste.entity.TraceEvent;
import com.waste.repository.TraceEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 追溯事件记录器：所有关键动作落库，支撑投诉追责还原 */
@Component
@RequiredArgsConstructor
public class TraceLogger {

    private final TraceEventRepository traceEventRepository;

    /**
     * @param appointmentId 关联预约（可空）
     * @param entityType    单据类型
     * @param entityId      单据 ID
     * @param actor         操作人（系统事件传 null）
     * @param action        动作标识
     * @param detail        明细说明
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(Long appointmentId, String entityType, Long entityId,
                       Session actor, String action, String detail) {
        TraceEvent e = new TraceEvent();
        e.setAppointmentId(appointmentId);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        if (actor != null) {
            e.setActorId(actor.userId());
            e.setActorName(actor.name());
            e.setActorRole(actor.role().name());
        } else {
            e.setActorName("SYSTEM");
            e.setActorRole("SYSTEM");
        }
        e.setAction(action);
        e.setDetail(detail);
        traceEventRepository.save(e);
    }
}
