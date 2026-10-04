package com.community.waste.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * 小区门禁与车辆限行规则（按小区配置，可多条）。
 * 派单时同时校验：允许进场时间窗 + 星期限行尾号。
 */
@Entity
@Table(name = "access_rule")
public class AccessRule extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String communityCode = "DEFAULT";

    /** 规则名称 */
    @Column(nullable = false, length = 64)
    private String ruleName;

    /** 适用星期：1=周一...7=周日；0=每天 */
    @Column(nullable = false)
    private Integer weekday = 0;

    /** 该星期被限行的尾号，逗号分隔；空表示不限行 */
    @Column(length = 32)
    private String restrictedTailDigits;

    /** 允许进场时间窗起 */
    private LocalTime allowedFrom;

    /** 允许进场时间窗止（可跨零点） */
    private LocalTime allowedTo;

    private Boolean enabled = true;

    public String getCommunityCode() { return communityCode; }
    public void setCommunityCode(String communityCode) { this.communityCode = communityCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Integer getWeekday() { return weekday; }
    public void setWeekday(Integer weekday) { this.weekday = weekday; }
    public String getRestrictedTailDigits() { return restrictedTailDigits; }
    public void setRestrictedTailDigits(String restrictedTailDigits) { this.restrictedTailDigits = restrictedTailDigits; }
    public LocalTime getAllowedFrom() { return allowedFrom; }
    public void setAllowedFrom(LocalTime allowedFrom) { this.allowedFrom = allowedFrom; }
    public LocalTime getAllowedTo() { return allowedTo; }
    public void setAllowedTo(LocalTime allowedTo) { this.allowedTo = allowedTo; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
