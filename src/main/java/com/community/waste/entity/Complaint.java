package com.community.waste.entity;

import com.community.waste.domain.DomainEnums.ComplaintStatus;
import com.community.waste.domain.DomainEnums.ComplaintType;
import jakarta.persistence.*;

/** 邻居投诉 */
@Entity
@Table(name = "complaint", indexes = @Index(name = "idx_complaint_status", columnList = "status"))
public class Complaint extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String complaintNo;

    /** 投诉人信息（邻居，非业主本人也可） */
    @Column(length = 32)
    private String complainantName;

    @Column(length = 20)
    private String complainantPhone;

    @Column(length = 64)
    private String complainantLocation;

    @Column(name = "registration_id")
    private Long registrationId;

    private Long appointmentId;
    private Long pointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ComplaintType type;

    @Column(nullable = false, length = 512)
    private String content;

    @Column(length = 512)
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;

    private Long handledBy;
    private java.time.LocalDateTime handledAt;

    @Column(length = 512)
    private String handleResult;

    /** 责任认定：CREW/COMPANY/OWNER/NONE */
    @Column(length = 16)
    private String liableParty;

    private Long violationId;
    private Long billId;

    public String getComplaintNo() { return complaintNo; }
    public void setComplaintNo(String complaintNo) { this.complaintNo = complaintNo; }
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    public String getComplainantPhone() { return complainantPhone; }
    public void setComplainantPhone(String complainantPhone) { this.complainantPhone = complainantPhone; }
    public String getComplainantLocation() { return complainantLocation; }
    public void setComplainantLocation(String complainantLocation) { this.complainantLocation = complainantLocation; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getPointId() { return pointId; }
    public void setPointId(Long pointId) { this.pointId = pointId; }
    public ComplaintType getType() { return type; }
    public void setType(ComplaintType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public java.time.LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(java.time.LocalDateTime handledAt) { this.handledAt = handledAt; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public String getLiableParty() { return liableParty; }
    public void setLiableParty(String liableParty) { this.liableParty = liableParty; }
    public Long getViolationId() { return violationId; }
    public void setViolationId(Long violationId) { this.violationId = violationId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
}
