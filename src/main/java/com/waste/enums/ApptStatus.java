package com.waste.enums;

/** 清运预约状态 */
public enum ApptStatus {
    APPROVED,            // 审核通过，待派单
    REJECTED,            // 审核驳回
    REJECTED_CAPACITY,   // 因垃圾点容量接近上限未通过（提示错峰/临时围挡）
    DISPATCHED,          // 已派单
    COMPLETED,           // 已称重完成
    CANCELLED
}
