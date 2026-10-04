package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.domain.DomainEnums.BagStatus;
import com.community.waste.domain.DomainEnums.CapacityDecision;
import com.community.waste.domain.DomainEnums.PointStatus;
import com.community.waste.entity.WastePoint;
import com.community.waste.repository.WasteBagRepository;
import com.community.waste.repository.WastePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * 垃圾点公共空间容量规则：
 * 占用 = 该点位下尚未装车清走的袋（REPORTED/ACCEPTED/SCHEDULED）。
 * 接近上限 → 不简单通过，提示错峰堆放；满 → 拒绝；
 * 物业可设置临时围挡（有效时间内按系数扩大容量）。
 */
@Service
public class CapacityService {

    /** 尚未清走、仍占用公共空间的袋状态 */
    private static final EnumSet<BagStatus> OCCUPYING =
            EnumSet.of(BagStatus.REPORTED, BagStatus.ACCEPTED, BagStatus.SCHEDULED);

    private final WastePointRepository pointRepository;
    private final WasteBagRepository bagRepository;
    private final FeeService feeService;

    public CapacityService(WastePointRepository pointRepository,
                           WasteBagRepository bagRepository,
                           FeeService feeService) {
        this.pointRepository = pointRepository;
        this.bagRepository = bagRepository;
        this.feeService = feeService;
    }

    @Transactional
    public int occupiedBags(Long pointId) {
        return (int) bagRepository.countOccupyingAtPoint(pointId, OCCUPYING);
    }

    /** 当前有效容量（考虑临时围挡扩容系数） */
    public EffectiveCapacity effectiveCapacity(WastePoint point) {
        BigDecimal factor = feeService.get(point.getCommunityCode(),
                FeeService.ENCLOSURE_CAPACITY_FACTOR, BigDecimal.valueOf(1.5));
        boolean enclosureActive = point.getEnclosureUntil() != null
                && point.getEnclosureUntil().isAfter(LocalDateTime.now());
        int capacity = point.getCapacityBags();
        int effective = enclosureActive
                ? BigDecimal.valueOf(capacity).multiply(factor).setScale(0, java.math.RoundingMode.HALF_UP).intValue()
                : capacity;
        return new EffectiveCapacity(capacity, effective, enclosureActive);
    }

    /**
     * 评估新增 newBags 袋是否可入场堆放。
     */
    @Transactional
    public CapacityCheck evaluate(Long pointId, int newBags) {
        WastePoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new BusinessException("垃圾点不存在: " + pointId));
        int occupied = occupiedBags(pointId);
        EffectiveCapacity cap = effectiveCapacity(point);

        double currentRatio = cap.effective() == 0 ? 0 : (double) occupied / cap.effective();
        double projectedRatio = cap.effective() == 0 ? 1 : (double) (occupied + newBags) / cap.effective();

        if (point.getStatus() == PointStatus.FULL || projectedRatio >= 1.0) {
            return new CapacityCheck(point, occupied, newBags, cap, currentRatio, projectedRatio,
                    CapacityDecision.REJECTED_FULL,
                    "垃圾点 " + point.getName() + " 已满（占用 " + (occupied + newBags)
                            + "/" + cap.effective() + " 袋），禁止继续堆放；请由物业安排临时围挡或改约其他点位");
        }
        double threshold = point.getNearFullThreshold() == null ? 0.8 : point.getNearFullThreshold();
        if (projectedRatio >= threshold) {
            String tip = cap.enclosureActive()
                    ? "围挡扩容中，仍接近上限"
                    : "接近容量上限（占用 " + occupied + "/" + cap.effective()
                            + " 袋，" + String.format("%.0f%%", projectedRatio * 100) + "）";
            return new CapacityCheck(point, occupied, newBags, cap, currentRatio, projectedRatio,
                    CapacityDecision.PEAK_SHIFT,
                    tip + "：建议错峰堆放（避开当日 18:00-22:00 高峰，改至次日 8:00 前），"
                            + "或由物业在该点位安排临时围挡后扩容承接");
        }
        return new CapacityCheck(point, occupied, newBags, cap, currentRatio, projectedRatio,
                CapacityDecision.ACCEPTED,
                "容量充足（占用 " + (occupied + newBags) + "/" + cap.effective() + " 袋）");
    }

    /** 根据当前占用刷新点位状态 */
    @Transactional
    public void refreshPointStatus(Long pointId) {
        WastePoint point = pointRepository.findById(pointId).orElse(null);
        if (point == null) return;
        int occupied = occupiedBags(pointId);
        EffectiveCapacity cap = effectiveCapacity(point);
        double ratio = cap.effective() == 0 ? 0 : (double) occupied / cap.effective();
        double threshold = point.getNearFullThreshold() == null ? 0.8 : point.getNearFullThreshold();
        PointStatus newStatus;
        if (cap.enclosureActive()) {
            newStatus = PointStatus.ENCLOSURE;
        } else if (ratio >= 1.0) {
            newStatus = PointStatus.FULL;
        } else if (ratio >= threshold) {
            newStatus = PointStatus.NEAR_FULL;
        } else {
            newStatus = PointStatus.NORMAL;
        }
        point.setStatus(newStatus);
        pointRepository.save(point);
    }

    public record EffectiveCapacity(int baseBags, int effectiveBags, boolean enclosureActive) {
        public int effective() { return effectiveBags; }
        public int base() { return baseBags; }
        public boolean enclosureActive() { return enclosureActive; }
    }

    public record CapacityCheck(WastePoint point, int occupied, int newBags, EffectiveCapacity capacity,
                                double currentRatio, double projectedRatio,
                                CapacityDecision decision, String message) {
        public boolean accepted() { return decision == CapacityDecision.ACCEPTED; }
        public boolean peakShift() { return decision == CapacityDecision.PEAK_SHIFT; }
    }
}
