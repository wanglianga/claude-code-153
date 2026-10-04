package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.*;
import com.community.waste.mq.DomainEvent;
import com.community.waste.mq.EventPublisher;
import com.community.waste.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 违规认定 + 押金扣款 + 罚款账单。
 * 责任方为施工队/业主时：优先从押金扣除，不足部分挂账单待缴（押金补缴）。
 * 责任方为清运公司时：计入其应付/扣罚账单，结算时抵扣。
 */
@Service
public class ViolationService {

    private final ViolationRepository violationRepository;
    private final RegistrationRepository registrationRepository;
    private final DepositDeductionRepository deductionRepository;
    private final BillRepository billRepository;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public ViolationService(ViolationRepository violationRepository,
                            RegistrationRepository registrationRepository,
                            DepositDeductionRepository deductionRepository,
                            BillRepository billRepository,
                            TraceService traceService,
                            EventPublisher publisher) {
        this.violationRepository = violationRepository;
        this.registrationRepository = registrationRepository;
        this.deductionRepository = deductionRepository;
        this.billRepository = billRepository;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    @Transactional
    public Violation open(ViolationType type, ViolationSource source, String description,
                          Long registrationId, Long appointmentId, Long tripId, Long complaintId,
                          String liableParty, BigDecimal fineAmount, String evidence, boolean publishEvent) {
        Violation v = new Violation();
        v.setViolationNo(NoGenerator.next("VIO"));
        v.setType(type);
        v.setSource(source);
        v.setDescription(description);
        v.setRegistrationId(registrationId);
        v.setAppointmentId(appointmentId);
        v.setTripId(tripId);
        v.setComplaintId(complaintId);
        v.setLiableParty(liableParty == null ? "CREW" : liableParty);
        v.setFineAmount(fineAmount == null ? BigDecimal.ZERO : fineAmount);
        v.setEvidence(evidence);
        v.setStatus(ViolationStatus.OPEN);
        Violation saved = violationRepository.save(v);

        traceService.record(TraceType.VIOLATION_CONFIRMED, Role.PROPERTY.name(),
                "登记违规[" + saved.getViolationNo() + "]：" + type.label + "，责任方 "
                        + saved.getLiableParty() + "，待物业认定。" + description,
                registrationId, null, appointmentId, tripId, null, saved.getId(), complaintId);

        if (publishEvent) {
            publisher.publish("waste.violation.opened",
                    DomainEvent.of("VIOLATION_OPENED", description)
                            .ref(registrationId, appointmentId, tripId)
                            .put("violationId", saved.getId())
                            .put("type", type.name()));
        }
        return saved;
    }

    /** 物业认定违规成立并执行扣款/开单 */
    @Transactional
    public Violation confirm(Long violationId, Long operatorId) {
        Violation v = violationRepository.findById(violationId)
                .orElseThrow(() -> new BusinessException("违规记录不存在: " + violationId));
        if (v.getStatus() != ViolationStatus.OPEN) {
            throw new BusinessException("仅待认定违规可确认，当前 " + v.getStatus());
        }
        v.setStatus(ViolationStatus.CONFIRMED);
        v.setConfirmedBy(operatorId);
        v.setConfirmedAt(LocalDateTime.now());

        BigDecimal fine = v.getFineAmount() == null ? BigDecimal.ZERO : v.getFineAmount();
        if (fine.signum() > 0) {
            if ("COMPANY".equals(v.getLiableParty())) {
                Bill bill = createFineBill(v, BillPartyType.COMPANY, "清运公司履约扣款：" + v.getType().label,
                        String.valueOf(v.getTripId() == null ? "COMMUNITY" : "COMPANY"), fine);
                v.setBillId(bill.getId());
            } else {
                // OWNER / CREW：从业主装修押金扣（施工队责任按管理约定向业主押金追偿，再由业主向施工队追责）
                Long regId = v.getRegistrationId();
                if (regId == null) {
                    throw new BusinessException("违规缺少装修登记，无法扣押金：" + v.getViolationNo());
                }
                applyDepositOrBill(v, regId, fine);
            }
        }
        Violation saved = violationRepository.save(v);

        traceService.record(TraceType.VIOLATION_CONFIRMED, Role.PROPERTY.name(),
                "物业认定违规[" + saved.getViolationNo() + "]成立：" + saved.getType().label
                        + "，罚款 " + fine + " 元，责任方 " + saved.getLiableParty(),
                saved.getRegistrationId(), null, saved.getAppointmentId(), saved.getTripId(),
                saved.getBillId(), saved.getId(), saved.getComplaintId());
        return saved;
    }

    private void applyDepositOrBill(Violation v, Long regId, BigDecimal fine) {
        Registration reg = registrationRepository.findById(regId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + regId));
        BigDecimal remaining = reg.getDeposit().subtract(reg.getDepositDeducted());
        BigDecimal fromDeposit = remaining.min(fine);
        BigDecimal shortfall = fine.subtract(fromDeposit);

        if (fromDeposit.signum() > 0) {
            reg.setDepositDeducted(reg.getDepositDeducted().add(fromDeposit));
            registrationRepository.save(reg);

            DepositDeduction d = new DepositDeduction();
            d.setDeductionNo(NoGenerator.next("DD"));
            d.setRegistrationId(regId);
            d.setReasonType("VIOLATION");
            d.setViolationId(v.getId());
            d.setAmount(fromDeposit);
            d.setReason("违规[" + v.getViolationNo() + "]" + v.getType().label + " 押金抵扣");
            d.setCreatedBy(v.getConfirmedBy());
            d.setBalanceAfter(reg.getDeposit().subtract(reg.getDepositDeducted()));
            deductionRepository.save(d);
            v.setDeductionId(d.getId());
        }

        Bill bill = createFineBill(v, "CREW".equals(v.getLiableParty()) ? BillPartyType.CREW : BillPartyType.OWNER,
                "违规罚款（押金抵扣 " + fromDeposit + " 元）：" + v.getType().label,
                String.valueOf(regId), fine);
        bill.setRegistrationId(regId);
        if (fromDeposit.signum() > 0) {
            bill.setDepositApplied(fromDeposit);
        }
        if (shortfall.signum() > 0) {
            // 押金不足：账单挂起待补缴
            bill.setStatus(BillStatus.HELD);
            bill.setSummary(bill.getSummary() + "；押金不足，差 " + shortfall + " 元待补缴");
            Bill repay = new Bill();
            repay.setBillNo(NoGenerator.next("BIL"));
            repay.setBillType(BillType.DEPOSIT_REPAY);
            repay.setPartyType(BillPartyType.OWNER);
            repay.setPartyRef(String.valueOf(regId));
            repay.setRegistrationId(regId);
            repay.setViolationId(v.getId());
            repay.setAmount(shortfall);
            repay.setReceivable(true);
            repay.setStatus(BillStatus.UNPAID);
            repay.setSummary("押金余额不足，违规[" + v.getViolationNo() + "]罚款差额补缴");
            billRepository.save(repay);
        } else {
            bill.setStatus(BillStatus.PAID);
            bill.setPaidAt(LocalDateTime.now());
            bill.setPaidChannel("DEPOSIT");
        }
        billRepository.save(bill);
        v.setBillId(bill.getId());
    }

    private Bill createFineBill(Violation v, BillPartyType partyType, String summary, String partyRef, BigDecimal amount) {
        Bill b = new Bill();
        b.setBillNo(NoGenerator.next("BIL"));
        b.setBillType(BillType.FINE);
        b.setPartyType(partyType);
        b.setPartyRef(partyRef);
        b.setViolationId(v.getId());
        b.setComplaintId(v.getComplaintId());
        b.setAmount(amount);
        b.setReceivable(true);
        b.setStatus(BillStatus.UNPAID);
        b.setSummary(summary);
        return billRepository.save(b);
    }

    @Transactional
    public Violation reject(Long violationId, String reason) {
        Violation v = violationRepository.findById(violationId)
                .orElseThrow(() -> new BusinessException("违规记录不存在: " + violationId));
        v.setStatus(ViolationStatus.REJECTED);
        v.setDescription(v.getDescription() + "；驳回原因：" + reason);
        return violationRepository.save(v);
    }
}
