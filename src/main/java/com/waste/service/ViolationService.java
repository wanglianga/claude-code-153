package com.waste.service;

import com.waste.common.BizException;
import com.waste.common.NoGen;
import com.waste.common.TraceLogger;
import com.waste.entity.Bill;
import com.waste.entity.RenovationRegistration;
import com.waste.entity.Violation;
import com.waste.enums.BillStatus;
import com.waste.enums.BillType;
import com.waste.enums.ViolationStatus;
import com.waste.repository.BillRepository;
import com.waste.repository.RenovationRegistrationRepository;
import com.waste.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 违规处理：MQ 事件驱动押金扣款 + 施工队罚款账单 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationService {

    private final ViolationRepository violationRepo;
    private final RenovationRegistrationRepository regRepo;
    private final BillRepository billRepo;
    private final BillingService billingService;
    private final TraceLogger traceLogger;

    /** MQ 事件：违规产生 → 从业主押金扣款（不足部分物业垫付），并生成施工队罚款账单。
     *  REQUIRES_NEW：同步降级路径在 afterCommit 回调中执行，必须挂起外围已提交事务另起新事务。 */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processDeduction(Long violationId) {
        Violation v = violationRepo.findById(violationId).orElseThrow();
        if (v.getStatus() != ViolationStatus.PENDING) {
            return;
        }
        RenovationRegistration reg = regRepo.findById(v.getRegistrationId()).orElseThrow();
        BigDecimal fine = v.getDeductionAmount();
        String period = LocalDate.now().toString().substring(0, 7);

        BigDecimal deduct = reg.getDepositBalance().min(fine);
        if (deduct.compareTo(BigDecimal.ZERO) > 0) {
            reg.setDepositBalance(reg.getDepositBalance().subtract(deduct));
            regRepo.save(reg);
            billingService.saveDeduction(reg.getId(), v.getId(), null, "FINE", deduct,
                    "违规罚款[" + v.getType() + "]: " + v.getDescription(), reg.getDepositBalance());
        }
        BigDecimal gap = fine.subtract(deduct);
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            Bill advance = new Bill();
            advance.setBillNo(NoGen.of("BILL"));
            advance.setBillType(BillType.PROPERTY_ADVANCE);
            advance.setPartyName("物业");
            advance.setRegistrationId(reg.getId());
            advance.setAppointmentId(v.getAppointmentId());
            advance.setPeriod(period);
            advance.setAmount(gap);
            advance.setDetail("押金不足，物业垫付违规罚款（违规单 " + v.getViolationNo() + "）");
            billRepo.save(advance);
        }

        // 施工队罚款账单（施工队向物业/业主赔付）
        Bill teamBill = new Bill();
        teamBill.setBillNo(NoGen.of("BILL"));
        teamBill.setBillType(BillType.CONSTRUCTION_TEAM);
        teamBill.setPartyName(reg.getConstructionTeam());
        teamBill.setRegistrationId(reg.getId());
        teamBill.setAppointmentId(v.getAppointmentId());
        teamBill.setPeriod(period);
        teamBill.setAmount(fine);
        teamBill.setDetail("施工队违规罚款[" + v.getType() + "]: " + v.getDescription()
                + "（缴清后返还业主押金）");
        billRepo.save(teamBill);

        v.setStatus(ViolationStatus.DEDUCTED);
        violationRepo.save(v);
        traceLogger.record(v.getAppointmentId(), "VIOLATION", v.getId(), null, "VIOLATION_DEDUCTED",
                "违规扣款完成: 押金扣除 " + deduct + " 元" + (gap.compareTo(BigDecimal.ZERO) > 0 ? "，物业垫付 " + gap + " 元" : "")
                        + "，施工队罚款账单 " + teamBill.getBillNo() + " " + fine + " 元");
    }

    /** 物业豁免违规（需在扣款前操作） */
    @Transactional
    public Violation waive(Long violationId, com.waste.auth.Session property) {
        Violation v = violationRepo.findById(violationId)
                .orElseThrow(() -> new BizException(404, "违规记录不存在"));
        if (v.getStatus() != ViolationStatus.PENDING) {
            throw new BizException("违规已处理（" + v.getStatus() + "），不能豁免");
        }
        v.setStatus(ViolationStatus.WAIVED);
        violationRepo.save(v);
        traceLogger.record(v.getAppointmentId(), "VIOLATION", v.getId(), property, "VIOLATION_WAIVED",
                "物业豁免违规 " + v.getViolationNo());
        return v;
    }

    public List<Violation> list(Long registrationId) {
        if (registrationId != null) {
            return violationRepo.findByRegistrationIdOrderByIdDesc(registrationId);
        }
        return violationRepo.findAll();
    }
}
