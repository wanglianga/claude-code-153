package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.Bill;
import com.community.waste.entity.Dispute;
import com.community.waste.mq.EventPublisher;
import com.community.waste.mq.DomainEvent;
import com.community.waste.domain.DomainEnums.TraceType;
import com.community.waste.repository.BillRepository;
import com.community.waste.repository.DisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用争议：业主/施工队/清运公司对账单提出异议，物业+财务裁决并调整账单。
 */
@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final BillRepository billRepository;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public DisputeService(DisputeRepository disputeRepository,
                          BillRepository billRepository,
                          TraceService traceService,
                          EventPublisher publisher) {
        this.disputeRepository = disputeRepository;
        this.billRepository = billRepository;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    @Transactional
    public Dispute raise(Long billId, String raisedByRole, String raisedByName, String reason, String evidence) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("账单不存在: " + billId));
        Dispute d = new Dispute();
        d.setDisputeNo(NoGenerator.next("DSP"));
        d.setBillId(billId);
        d.setRegistrationId(bill.getRegistrationId());
        d.setRaisedByRole(raisedByRole);
        d.setRaisedByName(raisedByName);
        d.setReason(reason);
        d.setEvidence(evidence);
        d.setStatus(DisputeStatus.OPEN);
        d = disputeRepository.save(d);

        bill.setStatus(BillStatus.DISPUTED);
        billRepository.save(bill);

        traceService.record(TraceType.DISPUTE, raisedByRole,
                raisedByName + " 对账单 " + bill.getBillNo() + "（" + bill.getAmount() + " 元）提出争议："
                        + reason,
                bill.getRegistrationId(), null, null, bill.getTripId(), bill.getId(), null, null);
        publisher.publish("waste.bill.dispute",
                DomainEvent.of("DISPUTE_RAISED", reason)
                        .ref(bill.getRegistrationId(), null, bill.getTripId())
                        .put("billId", billId).put("disputeId", d.getId()));
        return d;
    }

    @Transactional
    public Dispute review(Long disputeId, boolean approveAdjustment, BigDecimal adjustment,
                          String result, Long operatorId) {
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("争议不存在: " + disputeId));
        if (d.getStatus() != DisputeStatus.OPEN && d.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new BusinessException("争议已裁决，当前 " + d.getStatus());
        }
        Bill bill = billRepository.findById(d.getBillId())
                .orElseThrow(() -> new BusinessException("关联账单不存在"));

        d.setReviewedBy(operatorId);
        d.setReviewedAt(LocalDateTime.now());
        d.setReviewResult(result);

        if (approveAdjustment) {
            if (adjustment == null) {
                throw new BusinessException("批准调整必须给出调整金额（负数减收，正数补收）");
            }
            if (bill.getAmount().add(bill.getAdjustment()).add(adjustment).signum() < 0) {
                throw new BusinessException("调整后账单金额不能为负");
            }
            d.setStatus(DisputeStatus.APPROVED_ADJUSTMENT);
            d.setAdjustment(adjustment);
            bill.setAdjustment(bill.getAdjustment().add(adjustment));
            bill.setStatus(BillStatus.UNPAID);
        } else {
            d.setStatus(DisputeStatus.REJECTED);
            bill.setStatus(BillStatus.UNPAID);
        }
        billRepository.save(bill);
        d = disputeRepository.save(d);

        traceService.record(TraceType.DISPUTE_RESOLVED, Role.FINANCE.name(),
                "争议[" + d.getDisputeNo() + "]裁决："
                        + (approveAdjustment ? "批准调整 " + adjustment + " 元，账单应付 "
                        + bill.payableAmount() + " 元" : "驳回，维持原账 ")
                        + "。依据：" + result,
                bill.getRegistrationId(), null, null, bill.getTripId(), bill.getId(), null, null);
        return d;
    }

    @Transactional
    public Dispute underReview(Long disputeId) {
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("争议不存在: " + disputeId));
        d.setStatus(DisputeStatus.UNDER_REVIEW);
        return disputeRepository.save(d);
    }
}
