package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.RegistrationStatus;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.domain.DomainEnums.TraceType;
import com.community.waste.entity.Registration;
import com.community.waste.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final RegistrationRepository repository;
    private final TraceService traceService;

    public RegistrationService(RegistrationRepository repository, TraceService traceService) {
        this.repository = repository;
        this.traceService = traceService;
    }

    @Transactional
    public Registration register(Registration r) {
        repository.findByCommunityCodeAndRoomNo(r.getCommunityCode(), r.getRoomNo())
                .ifPresent(existing -> {
                    if (existing.getStatus() != RegistrationStatus.CLOSED
                            && existing.getStatus() != RegistrationStatus.REJECTED) {
                        throw new BusinessException("该房号已有进行中的装修登记：" + existing.getRoomNo());
                    }
                });
        if (r.getExpectedEndDate().isBefore(r.getStartDate())) {
            throw new BusinessException("预计工期结束日期不能早于开始日期");
        }
        r.setStatus(RegistrationStatus.REGISTERED);
        Registration saved = repository.save(r);
        traceService.record(TraceType.REGISTERED, Role.PROPERTY.name(),
                "业主 " + saved.getOwnerName() + " 在物业登记：房号 " + saved.getBuildingNo()
                        + "-" + saved.getRoomNo() + "，施工队 " + saved.getCrewName()
                        + "，工期 " + saved.getStartDate() + "~" + saved.getExpectedEndDate()
                        + "，押金 " + saved.getDeposit() + " 元，清运频次 " + saved.getHaulFrequency(),
                saved.getId(), null, null, null, null, null, null);
        return saved;
    }

    @Transactional
    public Registration approve(Long id, Long operatorId) {
        Registration r = mustGet(id);
        if (r.getStatus() != RegistrationStatus.REGISTERED) {
            throw new BusinessException("仅待审核登记可批准，当前状态 " + r.getStatus());
        }
        r.setStatus(RegistrationStatus.APPROVED);
        r.setApprovedBy(operatorId);
        r.setApprovedAt(LocalDateTime.now());
        Registration saved = repository.save(r);
        traceService.record(TraceType.AUDITED, Role.PROPERTY.name(),
                "物业审核通过装修登记，准予堆放并纳入清运计划",
                saved.getId(), null, null, null, null, null, null);
        return saved;
    }

    @Transactional
    public Registration reject(Long id, String reason) {
        Registration r = mustGet(id);
        r.setStatus(RegistrationStatus.REJECTED);
        r.setRejectReason(reason);
        repository.save(r);
        traceService.record(TraceType.AUDITED, Role.PROPERTY.name(),
                "物业驳回装修登记：" + reason, r.getId(), null, null, null, null, null, null);
        return r;
    }

    @Transactional
    public Registration close(Long id) {
        Registration r = mustGet(id);
        r.setStatus(RegistrationStatus.CLOSED);
        return repository.save(r);
    }

    public Registration mustGet(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("装修登记不存在: " + id));
    }
}
