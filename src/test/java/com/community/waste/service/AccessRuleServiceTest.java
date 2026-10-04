package com.community.waste.service;

import com.community.waste.entity.AccessRule;
import com.community.waste.entity.Vehicle;
import com.community.waste.repository.AccessRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessRuleServiceTest {

    private AccessRuleService service;
    private AccessRuleRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(AccessRuleRepository.class);
        service = new AccessRuleService(repository);
        AccessRule window = new AccessRule();
        window.setCommunityCode("DEFAULT");
        window.setRuleName("作业时间窗");
        window.setWeekday(0);
        window.setAllowedFrom(java.time.LocalTime.of(8, 0));
        window.setAllowedTo(java.time.LocalTime.of(17, 0));
        window.setEnabled(true);

        AccessRule monday = new AccessRule();
        monday.setCommunityCode("DEFAULT");
        monday.setRuleName("周一限行");
        monday.setWeekday(1);
        monday.setRestrictedTailDigits("1,6");
        monday.setEnabled(true);

        when(repository.findByCommunityCodeAndEnabledTrue("DEFAULT")).thenReturn(List.of(window, monday));
    }

    private Vehicle vehicle(String tail) {
        Vehicle v = new Vehicle();
        v.setPlate("京A" + tail + "0000");
        v.setTailDigit(tail);
        v.setCapacityBags(50);
        v.setCapacityKg(2000);
        v.setEnabled(true);
        return v;
    }

    @Test
    void allowsWithinTimeWindow() {
        // 2026-10-07 是周三，09:30 在窗口内，无尾号限制
        LocalDateTime wednesdayMorning = LocalDateTime.of(2026, 10, 7, 9, 30);
        assertThat(service.checkEntry("DEFAULT", vehicle("5"), wednesdayMorning)).isEmpty();
    }

    @Test
    void rejectsOutsideTimeWindow() {
        LocalDateTime night = LocalDateTime.of(2026, 10, 7, 20, 0);
        List<String> issues = service.checkEntry("DEFAULT", vehicle("5"), night);
        assertThat(issues).anyMatch(s -> s.contains("作业时间窗"));
    }

    @Test
    void enforcesTailDigitRestrictionOnMonday() {
        // 2026-10-05 是周一，尾号 1 限行；10:00 在时间窗内
        LocalDateTime mondayMorning = LocalDateTime.of(2026, 10, 5, 10, 0);
        assertThat(service.checkEntry("DEFAULT", vehicle("1"), mondayMorning))
                .anyMatch(s -> s.contains("尾号 1 限行"));
        assertThat(service.checkEntry("DEFAULT", vehicle("5"), mondayMorning)).isEmpty();
    }

    @Test
    void findsNextAllowedEntryAcrossNightWindow() {
        LocalDateTime next = service.nextAllowedEntry("DEFAULT", vehicle("5"),
                LocalDateTime.of(2026, 10, 7, 20, 0));
        assertThat(next).isEqualTo(LocalDateTime.of(2026, 10, 8, 8, 0));
    }
}
