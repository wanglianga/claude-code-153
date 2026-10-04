package com.community.waste.service;

import com.community.waste.domain.DomainEnums.TraceType;
import com.community.waste.entity.TraceEvent;
import com.community.waste.repository.TraceEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 全链路追溯：每个关键动作都留痕。 */
@Service
public class TraceService {

    private final TraceEventRepository repository;

    public TraceService(TraceEventRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录追溯事件。
     * 参数顺序：类型, 操作角色, 详情, 装修登记ID, 袋编码, 预约ID, 车次ID, 账单ID, 违规ID, 投诉ID
     */
    @Transactional
    public TraceEvent record(TraceType type, String actorRole, String detail,
                             Long registrationId, String bagCode, Long appointmentId, Long tripId,
                             Long billId, Long violationId, Long complaintId) {
        TraceEvent e = new TraceEvent(type, detail);
        e.setActorRole(actorRole);
        e.setRegistrationId(registrationId);
        e.setBagCode(bagCode);
        e.setAppointmentId(appointmentId);
        e.setTripId(tripId);
        e.setBillId(billId);
        e.setViolationId(violationId);
        e.setComplaintId(complaintId);
        return repository.save(e);
    }

    @Transactional
    public TraceEvent record(TraceType type, String actorRole, String detail) {
        return record(type, actorRole, detail, null, null, null, null, null, null, null);
    }

    public List<TraceEvent> byBag(String bagCode) {
        return repository.findByBagCodeOrderByCreatedAtAsc(bagCode);
    }

    public List<TraceEvent> byRegistration(Long registrationId) {
        return repository.findByRegistrationIdOrderByCreatedAtAsc(registrationId);
    }

    public List<TraceEvent> byAppointment(Long appointmentId) {
        return repository.findByAppointmentIdOrderByCreatedAtAsc(appointmentId);
    }

    public List<TraceEvent> byTrip(Long tripId) {
        return repository.findByTripIdOrderByCreatedAtAsc(tripId);
    }
}
