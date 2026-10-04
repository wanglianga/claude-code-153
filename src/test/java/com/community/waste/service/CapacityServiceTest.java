package com.community.waste.service;

import com.community.waste.domain.DomainEnums.BagStatus;
import com.community.waste.domain.DomainEnums.CapacityDecision;
import com.community.waste.entity.WastePoint;
import com.community.waste.repository.FeeConfigRepository;
import com.community.waste.repository.WasteBagRepository;
import com.community.waste.repository.WastePointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapacityServiceTest {

    private WasteBagRepository bagRepository;
    private WastePointRepository pointRepository;
    private CapacityService capacityService;
    private WastePoint point;

    @BeforeEach
    void setUp() {
        bagRepository = mock(WasteBagRepository.class);
        pointRepository = mock(WastePointRepository.class);
        FeeConfigRepository feeConfigRepository = mock(FeeConfigRepository.class);
        when(feeConfigRepository.findByCommunityCodeAndParamKey(any(), any())).thenReturn(Optional.empty());
        FeeService feeService = new FeeService(feeConfigRepository);
        capacityService = new CapacityService(pointRepository, bagRepository, feeService);

        point = new WastePoint();
        point.setId(1L);
        point.setCode("P-A");
        point.setName("A区垃圾点");
        point.setCapacityBags(100);
        point.setNearFullThreshold(0.8);
        when(pointRepository.findById(1L)).thenReturn(Optional.of(point));
    }

    private void givenOccupied(int n) {
        when(bagRepository.countOccupyingAtPoint(eq(1L), any())).thenReturn((long) n);
    }

    @Test
    void acceptsWhenBelowThreshold() {
        givenOccupied(50);
        CapacityService.CapacityCheck check = capacityService.evaluate(1L, 10);
        assertThat(check.decision()).isEqualTo(CapacityDecision.ACCEPTED);
    }

    @Test
    void requiresPeakShiftWhenNearFull() {
        givenOccupied(78);
        // 78 + 5 = 83 >= 80%
        CapacityService.CapacityCheck check = capacityService.evaluate(1L, 5);
        assertThat(check.decision()).isEqualTo(CapacityDecision.PEAK_SHIFT);
        assertThat(check.message()).contains("错峰");
    }

    @Test
    void rejectsWhenFull() {
        givenOccupied(98);
        // 98 + 5 = 103 > 100
        CapacityService.CapacityCheck check = capacityService.evaluate(1L, 5);
        assertThat(check.decision()).isEqualTo(CapacityDecision.REJECTED_FULL);
        assertThat(check.message()).contains("已满");
    }

    @Test
    void enclosureExpandsEffectiveCapacity() {
        point.setEnclosureUntil(java.time.LocalDateTime.now().plusHours(4));
        givenOccupied(100);
        // 默认围挡系数 1.5 -> 有效容量 150；当前占用 100 => 66.7%，新增 20 后 80%
        CapacityService.CapacityCheck check = capacityService.evaluate(1L, 20);
        assertThat(check.capacity().enclosureActive()).isTrue();
        assertThat(check.capacity().effective()).isEqualTo(150);
        assertThat(check.decision()).isEqualTo(CapacityDecision.PEAK_SHIFT);
    }
}
