package com.community.waste.service;

import com.community.waste.entity.AccessRule;
import com.community.waste.entity.Vehicle;
import com.community.waste.repository.AccessRuleRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 门禁与限行规则：
 * 1) 进场时间窗（可跨零点）；2) 星期限行尾号。
 */
@Service
public class AccessRuleService {

    private final AccessRuleRepository ruleRepository;

    public AccessRuleService(AccessRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * 校验车辆能否在指定时间进场。
     * @return 违规原因列表，为空表示全部通过
     */
    public List<String> checkEntry(String communityCode, Vehicle vehicle, LocalDateTime time) {
        List<String> violations = new ArrayList<>();
        List<AccessRule> rules = ruleRepository.findByCommunityCodeAndEnabledTrue(communityCode);
        // 小区未配规则时尝试 DEFAULT
        if (rules.isEmpty() && !"DEFAULT".equals(communityCode)) {
            rules = ruleRepository.findByCommunityCodeAndEnabledTrue("DEFAULT");
        }
        int weekday = time.getDayOfWeek().getValue(); // 1..7
        LocalTime t = time.toLocalTime();
        long windowRules = 0;
        boolean inAnyWindow = false;
        for (AccessRule rule : rules) {
            if (rule.getWeekday() != 0 && rule.getWeekday() != weekday) {
                continue;
            }
            if (rule.getRestrictedTailDigits() != null && !rule.getRestrictedTailDigits().isBlank()) {
                for (String d : rule.getRestrictedTailDigits().split(",")) {
                    if (d.trim().equals(vehicle.getTailDigit())) {
                        violations.add(rule.getRuleName() + "：星期" + weekday + " 尾号 "
                                + vehicle.getTailDigit() + " 限行");
                    }
                }
            }
            if (rule.getAllowedFrom() != null && rule.getAllowedTo() != null) {
                windowRules++;
                if (inWindow(t, rule.getAllowedFrom(), rule.getAllowedTo())) {
                    inAnyWindow = true;
                }
            }
        }
        // 多条时间窗为“任一窗口允许”（OR）；没有时间窗规则则不限制
        if (windowRules > 0 && !inAnyWindow) {
            violations.add("当前时间 " + t + " 不在小区允许清运车进场的任一作业时间窗内");
        }
        return violations;
    }

    /** 时间窗是否可跨零点 */
    private boolean inWindow(LocalTime t, LocalTime from, LocalTime to) {
        if (from.isBefore(to) || from.equals(to)) {
            return !t.isBefore(from) && !t.isAfter(to);
        }
        // 跨零点，如 20:00-06:00
        return !t.isBefore(from) || !t.isAfter(to);
    }

    /** 找从 from 起、下一个可进场时间（同小时粒度），最多向后探测7天 */
    public LocalDateTime nextAllowedEntry(String communityCode, Vehicle vehicle, LocalDateTime from) {
        LocalDateTime candidate = from.withMinute(0).withSecond(0).withNano(0);
        for (int h = 0; h < 24 * 7; h++) {
            if (checkEntry(communityCode, vehicle, candidate).isEmpty()) {
                return candidate;
            }
            candidate = candidate.plusHours(1);
        }
        return null;
    }

    /** 返回可读的限行提示（含 DayOfWeek，供 API 使用） */
    public String describeWeekday(DayOfWeek day) {
        return "周" + "一二三四五六日".charAt(day.getValue() - 1);
    }
}
