package com.waste.service;

import com.waste.auth.Session;
import com.waste.common.BizException;
import com.waste.common.NoGen;
import com.waste.common.TraceLogger;
import com.waste.entity.DepositDeduction;
import com.waste.entity.RenovationRegistration;
import com.waste.enums.RegStatus;
import com.waste.enums.Role;
import com.waste.repository.DepositDeductionRepository;
import com.waste.repository.RenovationRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RenovationRegistrationRepository regRepo;
    private final DepositDeductionRepository deductionRepo;
    private final TraceLogger traceLogger;

    @Transactional
    public RenovationRegistration create(Session owner, String building, String roomNumber, String constructionTeam,
                                         LocalDate start, LocalDate end, String wasteTypes,
                                         com.waste.enums.Frequency frequency, BigDecimal deposit) {
        if (end.isBefore(start)) {
            throw new BizException("预计完工日期不能早于开工日期");
        }
        if (deposit == null || deposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("押金金额必须大于 0");
        }
        RenovationRegistration r = new RenovationRegistration();
        r.setRegNo(NoGen.of("REG"));
        r.setOwnerId(owner.userId());
        r.setOwnerName(owner.name());
        r.setBuilding(building);
        r.setRoomNumber(roomNumber);
        r.setConstructionTeam(constructionTeam);
        r.setExpectedStartDate(start);
        r.setExpectedEndDate(end);
        r.setWasteTypes(wasteTypes);
        r.setFrequency(frequency);
        r.setDepositAmount(deposit);
        r.setDepositBalance(deposit);
        regRepo.save(r);
        traceLogger.record(null, "REGISTRATION", r.getId(), owner, "REGISTER",
                "装修登记: " + building + "栋" + roomNumber + "室, 施工队=" + constructionTeam
                        + ", 垃圾类型=" + wasteTypes + ", 清运频次=" + frequency + ", 押金=" + deposit);
        return r;
    }

    public List<RenovationRegistration> list(Session session, String building) {
        if (session.role() == Role.OWNER) {
            return regRepo.findByOwnerIdOrderByIdDesc(session.userId());
        }
        if (building != null && !building.isBlank()) {
            return regRepo.findByBuilding(building);
        }
        return regRepo.findAll();
    }

    public RenovationRegistration get(Long id) {
        return regRepo.findById(id).orElseThrow(() -> new BizException(404, "装修登记不存在"));
    }

    @Transactional
    public RenovationRegistration complete(Session session, Long id) {
        RenovationRegistration r = get(id);
        if (session.role() == Role.OWNER && !r.getOwnerId().equals(session.userId())) {
            throw new BizException(403, "只能操作本人的装修登记");
        }
        if (r.getStatus() != RegStatus.ACTIVE) {
            throw new BizException("登记已完结，不能重复操作");
        }
        r.setStatus(RegStatus.COMPLETED);
        regRepo.save(r);
        traceLogger.record(null, "REGISTRATION", r.getId(), session, "COMPLETE",
                "装修完工，押金余额=" + r.getDepositBalance());
        return r;
    }

    public List<DepositDeduction> depositLog(Session session, Long id) {
        RenovationRegistration r = get(id);
        if (session.role() == Role.OWNER && !r.getOwnerId().equals(session.userId())) {
            throw new BizException(403, "只能查看本人的押金流水");
        }
        return deductionRepo.findByRegistrationIdOrderByIdDesc(id);
    }
}
