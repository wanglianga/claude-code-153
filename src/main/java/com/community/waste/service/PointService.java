package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.domain.DomainEnums.PointStatus;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.domain.DomainEnums.TraceType;
import com.community.waste.entity.WastePoint;
import com.community.waste.repository.WastePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 垃圾点维护与临时围挡 */
@Service
public class PointService {

    private final WastePointRepository repository;
    private final CapacityService capacityService;
    private final TraceService traceService;

    public PointService(WastePointRepository repository, CapacityService capacityService,
                        TraceService traceService) {
        this.repository = repository;
        this.capacityService = capacityService;
        this.traceService = traceService;
    }

    @Transactional
    public WastePoint create(WastePoint point) {
        repository.findByCode(point.getCode()).ifPresent(p -> {
            throw new BusinessException("点位编码已存在: " + point.getCode());
        });
        return repository.save(point);
    }

    @Transactional
    public WastePoint update(WastePoint point) {
        WastePoint existing = repository.findById(point.getId())
                .orElseThrow(() -> new BusinessException("点位不存在: " + point.getId()));
        existing.setName(point.getName());
        existing.setLocation(point.getLocation());
        existing.setCapacityBags(point.getCapacityBags());
        existing.setCapacityKg(point.getCapacityKg());
        existing.setNearFullThreshold(point.getNearFullThreshold());
        return repository.save(existing);
    }

    /** 物业安排临时围挡：围挡期内容量按系数扩大 */
    @Transactional
    public WastePoint setEnclosure(Long pointId, LocalDateTime until, String reason) {
        WastePoint p = repository.findById(pointId)
                .orElseThrow(() -> new BusinessException("点位不存在: " + pointId));
        if (until == null || !until.isAfter(LocalDateTime.now())) {
            throw new BusinessException("围挡解除时间必须晚于当前时间");
        }
        p.setEnclosureUntil(until);
        p.setStatus(PointStatus.ENCLOSURE);
        p = repository.save(p);
        capacityService.refreshPointStatus(pointId);
        traceService.record(TraceType.ENCLOSURE, Role.PROPERTY.name(),
                "物业在垃圾点 " + p.getName() + " 设置临时围挡至 " + until
                        + "（围挡期内按扩容系数承接），原因：" + reason,
                null, null, null, null, null, null, null);
        return p;
    }

    @Transactional
    public WastePoint clearEnclosure(Long pointId) {
        WastePoint p = repository.findById(pointId)
                .orElseThrow(() -> new BusinessException("点位不存在: " + pointId));
        p.setEnclosureUntil(null);
        p = repository.save(p);
        capacityService.refreshPointStatus(pointId);
        return p;
    }

    public List<WastePoint> all() {
        List<WastePoint> points = repository.findAll();
        points.forEach(p -> capacityService.refreshPointStatus(p.getId()));
        return points;
    }

    public record PointView(WastePoint point, int occupiedBags, int effectiveBags, double usageRatio) {}

    @Transactional
    public PointView view(Long pointId) {
        WastePoint p = repository.findById(pointId)
                .orElseThrow(() -> new BusinessException("点位不存在: " + pointId));
        capacityService.refreshPointStatus(pointId);
        p = repository.findById(pointId).orElse(p);
        int occupied = capacityService.occupiedBags(pointId);
        int effective = capacityService.effectiveCapacity(p).effective();
        double ratio = effective == 0 ? 0 : (double) occupied / effective;
        return new PointView(p, occupied, effective, ratio);
    }
}
