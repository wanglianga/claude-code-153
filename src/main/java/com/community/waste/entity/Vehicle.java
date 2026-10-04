package com.community.waste.entity;

import jakarta.persistence.*;

/** 清运车辆 */
@Entity
@Table(name = "vehicle")
public class Vehicle extends BaseEntity {
    @Column(nullable = false, unique = true, length = 16)
    private String plate;

    /** 车牌号尾号（用于限行规则） */
    @Column(nullable = false, length = 2)
    private String tailDigit;

    /** 核定载重 kg */
    @Column(nullable = false)
    private Integer capacityKg;

    /** 车厢容积可装标准袋数 */
    @Column(nullable = false)
    private Integer capacityBags;

    @Column(nullable = false, length = 64)
    private String companyName;

    @Column(nullable = false)
    private Boolean enabled = true;

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getTailDigit() { return tailDigit; }
    public void setTailDigit(String tailDigit) { this.tailDigit = tailDigit; }
    public Integer getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Integer capacityKg) { this.capacityKg = capacityKg; }
    public Integer getCapacityBags() { return capacityBags; }
    public void setCapacityBags(Integer capacityBags) { this.capacityBags = capacityBags; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
