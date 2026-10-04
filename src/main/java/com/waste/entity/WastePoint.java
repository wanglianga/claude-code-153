package com.waste.entity;

import jakarta.persistence.*;
import lombok.Data;

/** 小区装修垃圾堆放点（公共空间容量纳入规则） */
@Data
@Entity
@Table(name = "waste_point")
public class WastePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 128)
    private String location;

    @Column(nullable = false)
    private Integer capacityBags;   // 容量（袋）

    @Column(nullable = false)
    private Integer currentBags = 0; // 当前已堆放（袋）

    @Column(nullable = false)
    private Boolean tempFence = false; // 是否已安排临时围挡

    @Column(nullable = false)
    private Integer tempExtraCapacity = 0; // 临时围挡增加的容量（袋）

    /** 有效容量 */
    public int effectiveCapacity() {
        return capacityBags + (Boolean.TRUE.equals(tempFence) ? tempExtraCapacity : 0);
    }
}
