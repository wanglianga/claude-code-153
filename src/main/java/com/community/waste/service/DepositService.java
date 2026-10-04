package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.entity.DepositDeduction;
import com.community.waste.entity.Registration;
import com.community.waste.repository.DepositDeductionRepository;
import com.community.waste.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 押金：补缴、人工扣款、余额查询 */
@Service
public class DepositService {

    private final RegistrationRepository registrationRepository;
    private final DepositDeductionRepository deductionRepository;

    public DepositService(RegistrationRepository registrationRepository,
                          DepositDeductionRepository deductionRepository) {
        this.registrationRepository = registrationRepository;
        this.deductionRepository = deductionRepository;
    }

    @Transactional
    public Registration topUp(Long registrationId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("补缴金额必须大于0");
        }
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + registrationId));
        reg.setDeposit(reg.getDeposit().add(amount));
        return registrationRepository.save(reg);
    }

    @Transactional
    public DepositDeduction manualDeduct(Long registrationId, BigDecimal amount, String reason, Long operatorId) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("扣款金额必须大于0");
        }
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + registrationId));
        BigDecimal remaining = reg.getDeposit().subtract(reg.getDepositDeducted());
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("押金可扣余额不足：剩余 " + remaining + " 元，申请扣 " + amount + " 元");
        }
        reg.setDepositDeducted(reg.getDepositDeducted().add(amount));
        registrationRepository.save(reg);

        DepositDeduction d = new DepositDeduction();
        d.setDeductionNo(NoGenerator.next("DD"));
        d.setRegistrationId(registrationId);
        d.setReasonType("MANUAL");
        d.setAmount(amount);
        d.setReason(reason);
        d.setCreatedBy(operatorId);
        d.setBalanceAfter(reg.getDeposit().subtract(reg.getDepositDeducted()));
        return deductionRepository.save(d);
    }

    public BigDecimal balance(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + registrationId));
        return reg.getDeposit().subtract(reg.getDepositDeducted());
    }
}
