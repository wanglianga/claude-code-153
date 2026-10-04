package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.RegistrationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 业主装修登记（在物业处登记） */
@Entity
@Table(name = "registration",
        indexes = @Index(name = "uk_reg_room", columnList = "communityCode,roomNo"))
public class Registration extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String communityCode;

    @Column(nullable = false, length = 32)
    private String communityName;

    @Column(nullable = false, length = 16)
    private String buildingNo;

    @Column(nullable = false, length = 16)
    private String roomNo;

    @Column(nullable = false, length = 32)
    private String ownerName;

    @Column(length = 20)
    private String ownerPhone;

    @Column(nullable = false, length = 64)
    private String crewName;

    @Column(length = 20)
    private String crewContact;

    @Column(length = 20)
    private String crewPhone;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate expectedEndDate;

    /** 垃圾类型，逗号分隔：wood木板/glass玻璃/ceramic瓷砖/mixed混合 */
    @Column(nullable = false, length = 128)
    private String wasteTypes;

    /** 清运频次（如每日1次/隔日/每周2次） */
    @Column(nullable = false, length = 32)
    private String haulFrequency;

    /** 押金 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    /** 已扣押金合计 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositDeducted = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    private Long approvedBy;

    private java.time.LocalDateTime approvedAt;

    @Column(length = 255)
    private String rejectReason;

    public String getCommunityCode() { return communityCode; }
    public void setCommunityCode(String communityCode) { this.communityCode = communityCode; }
    public String getCommunityName() { return communityName; }
    public void setCommunityName(String communityName) { this.communityName = communityName; }
    public String getBuildingNo() { return buildingNo; }
    public void setBuildingNo(String buildingNo) { this.buildingNo = buildingNo; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getCrewName() { return crewName; }
    public void setCrewName(String crewName) { this.crewName = crewName; }
    public String getCrewContact() { return crewContact; }
    public void setCrewContact(String crewContact) { this.crewContact = crewContact; }
    public String getCrewPhone() { return crewPhone; }
    public void setCrewPhone(String crewPhone) { this.crewPhone = crewPhone; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }
    public String getWasteTypes() { return wasteTypes; }
    public void setWasteTypes(String wasteTypes) { this.wasteTypes = wasteTypes; }
    public String getHaulFrequency() { return haulFrequency; }
    public void setHaulFrequency(String haulFrequency) { this.haulFrequency = haulFrequency; }
    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
    public BigDecimal getDepositDeducted() { return depositDeducted; }
    public void setDepositDeducted(BigDecimal depositDeducted) { this.depositDeducted = depositDeducted; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public java.time.LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(java.time.LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
