package com.waste.service;

import com.waste.auth.Session;
import com.waste.common.BizException;
import com.waste.common.NoGen;
import com.waste.common.TraceLogger;
import com.waste.config.RabbitConfig;
import com.waste.config.WasteProperties;
import com.waste.entity.DailyReport;
import com.waste.entity.RenovationRegistration;
import com.waste.entity.Violation;
import com.waste.entity.WastePoint;
import com.waste.enums.*;
import com.waste.mq.EventBus;
import com.waste.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DailyReportRepository reportRepo;
    private final RenovationRegistrationRepository regRepo;
    private final WastePointRepository wastePointRepo;
    private final ViolationRepository violationRepo;
    private final WasteProperties props;
    private final EventBus eventBus;
    private final TraceLogger traceLogger;

    @Transactional
    public DailyReport submit(Session worker, Long registrationId, LocalDate reportDate, int bagCount,
                              int extraBagCount, String stackingLocation, Long wastePointId,
                              boolean wood, boolean glass, boolean tile, boolean mixedHousehold) {
        RenovationRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new BizException(404, "装修登记不存在"));
        if (reg.getStatus() != RegStatus.ACTIVE) {
            throw new BizException("该装修登记已完结，不能继续上报");
        }
        if (worker.teamName() != null && !worker.teamName().equals(reg.getConstructionTeam())) {
            throw new BizException(403, "只能为本人所在施工队的登记单上报");
        }
        if (bagCount <= 0) {
            throw new BizException("袋装数量必须大于 0");
        }
        if (extraBagCount < 0 || extraBagCount > bagCount) {
            throw new BizException("临时加袋数量不合法");
        }
        WastePoint point = wastePointRepo.findById(wastePointId)
                .orElseThrow(() -> new BizException(404, "垃圾点不存在"));
        checkFrequency(reg, reportDate);

        DailyReport r = new DailyReport();
        r.setRegistrationId(registrationId);
        r.setReportDate(reportDate);
        r.setBagCount(bagCount);
        r.setExtraBagCount(extraBagCount);
        r.setStackingLocation(stackingLocation);
        r.setWastePointId(wastePointId);
        r.setContainsWood(wood);
        r.setContainsGlass(glass);
        r.setContainsTile(tile);
        r.setContainsMixedHousehold(mixedHousehold);
        r.setSubmitterId(worker.userId());
        r.setSubmitterName(worker.name());
        reportRepo.save(r);

        traceLogger.record(null, "REPORT", r.getId(), worker, "SUBMIT_REPORT",
                "每日上报: " + bagCount + "袋(含临时加袋" + extraBagCount + "袋), 堆放于[" + stackingLocation
                        + "], 垃圾点=" + point.getName() + ", 分类: 木板=" + wood + " 玻璃=" + glass
                        + " 瓷砖=" + tile + " 混入生活垃圾=" + mixedHousehold);

        // 违规检测：消防通道堆放 / 混入生活垃圾
        if (stackingLocation.contains("消防")) {
            createViolation(reg, r, ViolationType.FIRE_LANE_STACKING,
                    "施工队将装修垃圾堆放至消防通道: " + stackingLocation, props.getFireLaneFine());
        }
        if (mixedHousehold) {
            createViolation(reg, r, ViolationType.MIXED_HOUSEHOLD_WASTE,
                    "装修垃圾中混入生活垃圾", props.getMixedWasteFine());
        }
        return r;
    }

    /** 清运频次约束：DAILY 每日一次 / EVERY_TWO_DAYS 每两日一次 / WEEKLY 每周一次 */
    private void checkFrequency(RenovationRegistration reg, LocalDate reportDate) {
        reportRepo.findTopByRegistrationIdOrderByReportDateDesc(reg.getId()).ifPresent(last -> {
            long days = ChronoUnit.DAYS.between(last.getReportDate(), reportDate);
            int interval = switch (reg.getFrequency()) {
                case DAILY -> 1;
                case EVERY_TWO_DAYS -> 2;
                case WEEKLY -> 7;
            };
            if (days < interval) {
                throw new BizException("清运频次为 " + reg.getFrequency() + "，距上次上报（"
                        + last.getReportDate() + "）不足 " + interval + " 天，不能重复上报");
            }
        });
    }

    private void createViolation(RenovationRegistration reg, DailyReport report,
                                 ViolationType type, String description, java.math.BigDecimal fine) {
        Violation v = new Violation();
        v.setViolationNo(NoGen.of("VIO"));
        v.setRegistrationId(reg.getId());
        v.setReportId(report.getId());
        v.setType(type);
        v.setDescription(description);
        v.setDeductionAmount(fine);
        violationRepo.save(v);
        traceLogger.record(null, "VIOLATION", v.getId(), null, "VIOLATION_DETECTED",
                "检测到违规[" + type + "]: " + description + "，拟扣款 " + fine + " 元");
        eventBus.publish(RabbitConfig.RK_VIOLATION_CREATED, v.getId());
    }

    /** 业主/施工队临时加袋（仅限待审核的上报单） */
    @Transactional
    public DailyReport addExtraBags(Session session, Long reportId, int extraCount) {
        if (extraCount <= 0) {
            throw new BizException("加袋数量必须大于 0");
        }
        DailyReport r = reportRepo.findById(reportId)
                .orElseThrow(() -> new BizException(404, "上报单不存在"));
        if (r.getStatus() != ReportStatus.PENDING) {
            throw new BizException("该上报单已审核，不能临时加袋");
        }
        RenovationRegistration reg = regRepo.findById(r.getRegistrationId()).orElseThrow();
        if (session.role() == Role.OWNER && !reg.getOwnerId().equals(session.userId())) {
            throw new BizException(403, "只能为本人房产的上报单加袋");
        }
        if (session.role() == Role.WORKER && session.teamName() != null
                && !session.teamName().equals(reg.getConstructionTeam())) {
            throw new BizException(403, "只能为本施工队的上报单加袋");
        }
        r.setBagCount(r.getBagCount() + extraCount);
        r.setExtraBagCount(r.getExtraBagCount() + extraCount);
        reportRepo.save(r);
        traceLogger.record(null, "REPORT", r.getId(), session, "EXTRA_BAGS",
                "临时加袋 " + extraCount + " 袋，当前共 " + r.getBagCount() + " 袋（加袋将按 "
                        + props.getExtraBagMultiplier() + " 倍费率计费）");
        return r;
    }

    public List<DailyReport> list(Session session, Long registrationId, ReportStatus status) {
        if (registrationId != null) {
            return reportRepo.findByRegistrationIdOrderByIdDesc(registrationId);
        }
        if (session.role() == Role.WORKER) {
            return reportRepo.findAll().stream()
                    .filter(r -> r.getSubmitterId().equals(session.userId())).toList();
        }
        if (session.role() == Role.OWNER) {
            List<Long> myRegIds = regRepo.findByOwnerIdOrderByIdDesc(session.userId())
                    .stream().map(RenovationRegistration::getId).toList();
            return reportRepo.findAll().stream()
                    .filter(r -> myRegIds.contains(r.getRegistrationId())).toList();
        }
        if (status != null) {
            return reportRepo.findByStatusOrderByIdDesc(status);
        }
        return reportRepo.findAll();
    }
}
