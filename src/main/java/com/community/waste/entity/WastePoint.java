package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.PointStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

/** 小区垃圾集中堆放点，容量纳入预约规则 */
@Entity
@Table(name = "waste_point")
public class WastePoint extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String communityCode = "DEFAULT";

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    /** 所属小区/楼栋位置描述 */
    @Column(length = 128)
    private String location;

    /** 容量上限（标准袋数） */
    @Column(nullable = false)
    private Integer capacityBags;

    /** 容量重量上限（kg），按袋均重估算 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityKg = BigDecimal.ZERO;

    /** 接近上限阈值比例，如 0.8 */
    @Column(nullable = false)
    private Double nearFullThreshold = 0.8;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PointStatus status = PointStatus.NORMAL;

    /** 临时围挡解除时间，为空表示无围挡 */
    private java.time.LocalDateTime enclosureUntil;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCommunityCode() { return communityCode; }
    public void setCommunityCode(String communityCode) { this.communityCode = communityCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getCapacityBags() { return capacityBags; }
    public void setCapacityBags(Integer capacityBags) { this.capacityBags = capacityBags; }
    public BigDecimal getCapacityKg() { return capacityKg; }
    public void setCapacityKg(BigDecimal capacityKg) { this.capacityKg = capacityKg; }
    public Double getNearFullThreshold() { return nearFullThreshold; }
    public void setNearFullThreshold(Double nearFullThreshold) { this.nearFullThreshold = nearFullThreshold; }
    public PointStatus getStatus() { return status; }
    public void setStatus(PointStatus status) { this.status = status; }
    public java.time.LocalDateTime getEnclosureUntil() { return enclosureUntil; }
    public void setEnclosureUntil(java.time.LocalDateTime enclosureUntil) { this.enclosureUntil = enclosureUntil; }
}
