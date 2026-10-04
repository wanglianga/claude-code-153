package com.community.waste.domain;

/** 领域枚举集中定义，使用方静态导入所需枚举。 */
public final class DomainEnums {
    private DomainEnums() {}

    /** 系统角色 */
    public enum Role {
        PROPERTY("物业"), COMPANY("清运公司"), FINANCE("财务"), CREW("施工队"), OWNER("业主"), SYSTEM("系统");
        public final String label;
        Role(String label) { this.label = label; }
    }

    /** 装修登记状态 */
    public enum RegistrationStatus { REGISTERED, APPROVED, REJECTED, CLOSED }

    /** 施工队日报状态 */
    public enum ReportStatus { SUBMITTED, APPROVED, REJECTED }

    /** 垃圾袋全链路状态 */
    public enum BagStatus {
        REPORTED("已上报"),
        ACCEPTED("物业已核验收"),
        SCHEDULED("已排入清运计划"),
        LOADED("已装车"),
        WEIGHED("已称重"),
        DISPOSED("处置场已回执");
        public final String label;
        BagStatus(String label) { this.label = label; }
    }

    /** 清运预约状态 */
    public enum AppointmentStatus {
        APPROVED("物业已审核"),
        DISPATCHED("已派单"),
        ARRIVED("车辆已到场"),
        LOADED("已装车离场"),
        COMPLETED("处置完成"),
        REJECTED("已驳回"),
        CANCELLED("已取消");
        public final String label;
        AppointmentStatus(String label) { this.label = label; }
    }

    /** 车次状态 */
    public enum TripStatus { PLANNED, ENROUTE, ENTERED, EXITED, RECEIVED, LATE, CANCELLED }

    public enum ViolationType {
        FIRE_LANE("堆占消防通道"),
        MIXED_HOUSEHOLD("混入生活垃圾"),
        SPILL_OVERDUE("车辆迟到致垃圾点溢出"),
        ILLEGAL_LOCATION("违规堆放"),
        EXTRA_UNREPORTED("临时加袋未报备"),
        OTHER("其他违规");
        public final String label;
        ViolationType(String label) { this.label = label; }
    }

    public enum ViolationSource { CREW_REPORT, INSPECTION, COMPLAINT, SYSTEM }

    public enum ViolationStatus { OPEN, CONFIRMED, REJECTED }

    public enum ComplaintType {
        ODOR("异味"), DUST("扬尘"), NOISE("噪音"), ILLEGAL_PILE("违规堆放"), MIXED("混入生活垃圾"), OTHER("其他");
        public final String label;
        ComplaintType(String label) { this.label = label; }
    }

    public enum ComplaintStatus { SUBMITTED, INVESTIGATING, RESOLVED, REJECTED }

    public enum BillPartyType {
        OWNER("业主"), CREW("施工队"), PROPERTY_ADVANCE("物业垫付"), COMPANY("清运公司");
        public final String label;
        BillPartyType(String label) { this.label = label; }
    }

    public enum BillType {
        DISPOSAL_FEE("清运费"), FINE("违规罚款"), ADVANCE_STATEMENT("物业垫付台账"),
        COMPANY_SETTLEMENT("清运公司结算"), DEPOSIT_REPAY("押金补缴");
        public final String label;
        BillType(String label) { this.label = label; }
    }

    public enum BillStatus { UNPAID, HELD, PAID, DISPUTED, CANCELLED }

    public enum DisputeStatus { OPEN, UNDER_REVIEW, APPROVED_ADJUSTMENT, REJECTED, WITHDRAWN }

    public enum AnomalyStatus { OPEN, CONFIRMED_NORMAL, ADJUSTED, IGNORED }

    public enum PointStatus { NORMAL, NEAR_FULL, FULL, ENCLOSURE }

    public enum CapacityDecision { ACCEPTED("可承接"), PEAK_SHIFT("建议错峰堆放"), ENCLOSURE("需安排临时围挡"), REJECTED_FULL("垃圾点已满");
        public final String label;
        CapacityDecision(String label) { this.label = label; }
    }

    /** 追溯事件类型：描述一袋垃圾的完整生命节点 */
    public enum TraceType {
        REGISTERED("装修登记"),
        REPORTED("施工队堆放上报"),
        EXTRA_BAGS("业主临时加袋"),
        AUDITED("物业审核"),
        APPOINTMENT_CREATED("生成清运预约"),
        DISPATCHED("清运公司派单"),
        VEHICLE_ENROUTE("车辆出发"),
        VEHICLE_ENTER("车辆进场"),
        BAGS_LOADED("装车核验"),
        VEHICLE_EXIT("车辆出场称重"),
        WEIGH_ANOMALY("称重异常"),
        DISPOSAL_RECEIPT("处置场回执"),
        VIOLATION_CONFIRMED("违规认定与扣款"),
        COMPLAINT("邻居投诉"),
        COMPLAINT_RESOLVED("投诉处理"),
        BILL_GENERATED("费用生成"),
        DISPUTE("费用争议"),
        DISPUTE_RESOLVED("争议裁决"),
        ENCLOSURE("临时围挡"),
        MANUAL("人工记录");
        public final String label;
        TraceType(String label) { this.label = label; }
    }
}
