package com.community.waste.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 计费/规则参数（按小区配置，缺省 communityCode=DEFAULT）。
 * 物业可调整单价与罚款标准；财务结算读取本配置。
 */
@Entity
@Table(name = "fee_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_cfg", columnNames = {"communityCode", "paramKey"}))
public class FeeConfig extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String communityCode = "DEFAULT";

    @Column(nullable = false, length = 48)
    private String paramKey;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal paramValue;

    @Column(length = 128)
    private String remark;

    public FeeConfig() {}
    public FeeConfig(String key, BigDecimal value, String remark) {
        this.paramKey = key;
        this.paramValue = value;
        this.remark = remark;
    }

    public String getCommunityCode() { return communityCode; }
    public void setCommunityCode(String communityCode) { this.communityCode = communityCode; }
    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }
    public BigDecimal getParamValue() { return paramValue; }
    public void setParamValue(BigDecimal paramValue) { this.paramValue = paramValue; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
