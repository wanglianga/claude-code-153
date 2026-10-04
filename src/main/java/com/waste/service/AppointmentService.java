package com.waste.service;

import com.waste.auth.Session;
import com.waste.common.BizException;
import com.waste.common.NoGen;
import com.waste.common.TraceLogger;
import com.waste.config.RabbitConfig;
import com.waste.config.WasteProperties;
import com.waste.entity.*;
import com.waste.enums.*;
import com.waste.mq.EventBus;
import com.waste.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final CollectionAppointmentRepository apptRepo;
    private final DailyReportRepository reportRepo;
    private final RenovationRegistrationRepository regRepo;
    private final WastePointRepository wastePointRepo;
    private final BagTraceRepository bagTraceRepo;
    private final WasteProperties props;
    private final EventBus eventBus;
    private final TraceLogger traceLogger;

    /**
     * 物业审核每日上报，生成清运预约。
     * 容量规则：垃圾点已堆放 + 本批袋数 超过 有效容量 × 预警比例 时不能简单通过，
     * 提示施工队错峰堆放或由物业安排临时围挡（围挡扩容后可重新审核通过）。
     */
    @Transactional
    public CollectionAppointment review(Session property, Long reportId, boolean approve,
                                        LocalDate apptDate, String timeSlot, String comment) {
        DailyReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new BizException(404, "上报单不存在"));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BizException("该上报单已审核，不能重复审核");
        }
        RenovationRegistration reg = regRepo.findById(report.getRegistrationId()).orElseThrow();
        WastePoint point = wastePointRepo.findById(report.getWastePointId()).orElseThrow();

        CollectionAppointment appt = new CollectionAppointment();
        appt.setApptNo(NoGen.of("APT"));
        appt.setRegistrationId(reg.getId());
        appt.setReportId(report.getId());
        appt.setWastePointId(point.getId());
        appt.setApptDate(apptDate);
        appt.setTimeSlot(timeSlot);
        appt.setReviewerId(property.userId());
        appt.setReviewerName(property.name());
        appt.setReviewComment(comment);
        appt.setReviewedAt(LocalDateTime.now());

        if (!approve) {
            appt.setStatus(ApptStatus.REJECTED);
            report.setStatus(ReportStatus.REJECTED);
            report.setReviewComment(comment);
            reportRepo.save(report);
            apptRepo.save(appt);
            traceLogger.record(appt.getId(), "APPOINTMENT", appt.getId(), property, "REVIEW_REJECT",
                    "审核驳回: " + comment);
            return appt;
        }

        // ---- 容量规则校验 ----
        int projected = point.getCurrentBags() + report.getBagCount();
        int effective = point.effectiveCapacity();
        BigDecimal warnLine = BigDecimal.valueOf(effective).multiply(props.getCapacityWarnRatio());
        boolean overWarn = BigDecimal.valueOf(projected).compareTo(warnLine) > 0;

        if (overWarn && projected > effective) {
            // 即使围挡扩容也放不下 → 必须错峰
            appt.setStatus(ApptStatus.REJECTED_CAPACITY);
            appt.setCapacityMessage("垃圾点[" + point.getName() + "]容量不足：当前 " + point.getCurrentBags()
                    + " 袋 / 有效容量 " + effective + " 袋，本批 " + report.getBagCount()
                    + " 袋将超出容量上限。请施工队错峰堆放（改约其他日期/时段），或由物业安排临时围挡扩容后重新审核。");
            apptRepo.save(appt);
            traceLogger.record(appt.getId(), "APPOINTMENT", appt.getId(), property, "REVIEW_REJECT_CAPACITY",
                    appt.getCapacityMessage());
            return appt;
        }
        if (overWarn && !Boolean.TRUE.equals(point.getTempFence())) {
            // 超过预警线且未设临时围挡 → 不能简单通过
            appt.setStatus(ApptStatus.REJECTED_CAPACITY);
            appt.setCapacityMessage("垃圾点[" + point.getName() + "]已接近容量上限：当前 " + point.getCurrentBags()
                    + " 袋 / 容量 " + point.getCapacityBags() + " 袋（预警线 "
                    + props.getCapacityWarnRatio().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()
                    + "%），本批 " + report.getBagCount() + " 袋将达到 " + projected
                    + " 袋。建议：1) 施工队错峰堆放（改约其他日期/时段）；2) 物业安排临时围挡扩容后重新审核。");
            apptRepo.save(appt);
            traceLogger.record(appt.getId(), "APPOINTMENT", appt.getId(), property, "REVIEW_REJECT_CAPACITY",
                    appt.getCapacityMessage());
            return appt;
        }

        // ---- 审核通过 ----
        appt.setStatus(ApptStatus.APPROVED);
        if (overWarn) {
            appt.setCapacityMessage("垃圾点[" + point.getName() + "]已启用临时围挡扩容（+" + point.getTempExtraCapacity()
                    + " 袋），容量校验通过：当前 " + point.getCurrentBags() + " 袋 + 本批 " + report.getBagCount()
                    + " 袋 = " + projected + " 袋 / 有效容量 " + effective + " 袋。");
        }
        apptRepo.save(appt);

        report.setStatus(ReportStatus.APPROVED);
        report.setReviewComment(comment);
        reportRepo.save(report);

        point.setCurrentBags(projected);
        wastePointRepo.save(point);

        // 生成袋级追溯单
        BagTrace trace = new BagTrace();
        trace.setTraceCode(NoGen.of("TRACE"));
        trace.setAppointmentId(appt.getId());
        trace.setRegistrationId(reg.getId());
        trace.setReportId(report.getId());
        trace.setStatus(TraceStatus.APPROVED);
        bagTraceRepo.save(trace);

        traceLogger.record(appt.getId(), "APPOINTMENT", appt.getId(), property, "REVIEW_APPROVE",
                "审核通过，生成清运预约 " + appt.getApptNo() + "（" + apptDate + " " + timeSlot + "），追溯码 "
                        + trace.getTraceCode() + (appt.getCapacityMessage() != null ? "；" + appt.getCapacityMessage() : ""));
        eventBus.publish(RabbitConfig.RK_APPOINTMENT_APPROVED, appt.getId());
        return appt;
    }

    public List<CollectionAppointment> list(Session session, ApptStatus status) {
        if (session.role() == Role.OWNER) {
            List<Long> myRegIds = regRepo.findByOwnerIdOrderByIdDesc(session.userId())
                    .stream().map(RenovationRegistration::getId).toList();
            return apptRepo.findAll().stream()
                    .filter(a -> myRegIds.contains(a.getRegistrationId())).toList();
        }
        if (session.role() == Role.WORKER) {
            List<Long> myRegIds = regRepo.findAll().stream()
                    .filter(r -> r.getConstructionTeam().equals(session.teamName()))
                    .map(RenovationRegistration::getId).toList();
            return apptRepo.findAll().stream()
                    .filter(a -> myRegIds.contains(a.getRegistrationId())).toList();
        }
        if (status != null) {
            return apptRepo.findByStatusOrderByIdDesc(status);
        }
        return apptRepo.findAll();
    }

    public CollectionAppointment get(Long id) {
        return apptRepo.findById(id).orElseThrow(() -> new BizException(404, "预约不存在"));
    }
}
