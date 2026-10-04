package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.TripStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 清运车次（派单/进出场/称重/联单） */
@Entity
@Table(name = "haul_trip",
        indexes = {@Index(name = "idx_trip_plate_time", columnList = "plate,enterTime"),
                @Index(name = "idx_trip_status", columnList = "status")})
public class HaulTrip extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String tripNo;

    /** 清运联单编号 */
    @Column(length = 40)
    private String manifestNo;

    @Column(nullable = false, length = 16)
    private String plate;

    @Column(length = 32)
    private String driverName;

    @Column(length = 20)
    private String driverPhone;

    private Long driverId;
    private Long vehicleId;

    /** 计划到场时间 */
    private LocalDateTime plannedArrival;

    /** 实际进场时间 */
    private LocalDateTime enterTime;

    /** 实际出场时间 */
    private LocalDateTime exitTime;

    /** 空车重量 kg */
    @Column(precision = 10, scale = 2)
    private BigDecimal tareWeight;

    /** 装载总重（出场重车重量） kg */
    @Column(precision = 10, scale = 2)
    private BigDecimal grossWeight;

    /** 净重 = gross - tare */
    @Column(precision = 10, scale = 2)
    private BigDecimal netWeight;

    /** 装车袋数（现场清点） */
    private Integer loadedBagCount;

    /** 现场照片（URL或相对路径，逗号分隔） */
    @Column(length = 1024)
    private String photos;

    /** 门禁放行记录 */
    @Column(length = 128)
    private String gateRecord;

    /** 计划路线/点位顺序描述 */
    @Column(length = 512)
    private String routePlan;

    /** 本车次覆盖的预约（逗号分隔 appointmentId） */
    @Column(length = 512)
    private String appointmentIds;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private TripStatus status = TripStatus.PLANNED;

    @Column(length = 255)
    private String remark;

    public String getTripNo() { return tripNo; }
    public void setTripNo(String tripNo) { this.tripNo = tripNo; }
    public String getManifestNo() { return manifestNo; }
    public void setManifestNo(String manifestNo) { this.manifestNo = manifestNo; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public LocalDateTime getPlannedArrival() { return plannedArrival; }
    public void setPlannedArrival(LocalDateTime plannedArrival) { this.plannedArrival = plannedArrival; }
    public LocalDateTime getEnterTime() { return enterTime; }
    public void setEnterTime(LocalDateTime enterTime) { this.enterTime = enterTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
    public BigDecimal getTareWeight() { return tareWeight; }
    public void setTareWeight(BigDecimal tareWeight) { this.tareWeight = tareWeight; }
    public BigDecimal getGrossWeight() { return grossWeight; }
    public void setGrossWeight(BigDecimal grossWeight) { this.grossWeight = grossWeight; }
    public BigDecimal getNetWeight() { return netWeight; }
    public void setNetWeight(BigDecimal netWeight) { this.netWeight = netWeight; }
    public Integer getLoadedBagCount() { return loadedBagCount; }
    public void setLoadedBagCount(Integer loadedBagCount) { this.loadedBagCount = loadedBagCount; }
    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }
    public String getGateRecord() { return gateRecord; }
    public void setGateRecord(String gateRecord) { this.gateRecord = gateRecord; }
    public String getRoutePlan() { return routePlan; }
    public void setRoutePlan(String routePlan) { this.routePlan = routePlan; }
    public String getAppointmentIds() { return appointmentIds; }
    public void setAppointmentIds(String appointmentIds) { this.appointmentIds = appointmentIds; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Transient
    public List<Long> appointmentIdList() {
        List<Long> ids = new ArrayList<>();
        if (appointmentIds == null || appointmentIds.isBlank()) return ids;
        for (String s : appointmentIds.split(",")) {
            if (!s.isBlank()) ids.add(Long.valueOf(s.trim()));
        }
        return ids;
    }
}
