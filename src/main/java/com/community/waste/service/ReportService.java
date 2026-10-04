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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 施工队每日堆放上报、物业审核（审核后生成清运预约）、业主临时加袋。
 */
@Service
public class ReportService {

    private final DailyReportRepository reportRepository;
    private final RegistrationRepository registrationRepository;
    private final WasteBagRepository bagRepository;
    private final WastePointRepository pointRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBagRepository appointmentBagRepository;
    private final CapacityService capacityService;
    private final ViolationService violationService;
    private final FeeService feeService;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public ReportService(DailyReportRepository reportRepository,
                         RegistrationRepository registrationRepository,
                         WasteBagRepository bagRepository,
                         WastePointRepository pointRepository,
                         AppointmentRepository appointmentRepository,
                         AppointmentBagRepository appointmentBagRepository,
                         CapacityService capacityService,
                         ViolationService violationService,
                         FeeService feeService,
                         TraceService traceService,
                         EventPublisher publisher) {
        this.reportRepository = reportRepository;
        this.registrationRepository = registrationRepository;
        this.bagRepository = bagRepository;
        this.pointRepository = pointRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentBagRepository = appointmentBagRepository;
        this.capacityService = capacityService;
        this.violationService = violationService;
        this.feeService = feeService;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    public record SubmitResult(DailyReport report, List<WasteBag> bags, String notice) {}

    /**
     * 施工队提交当日堆放上报。
     * @param acknowledgePeakShift 容量接近上限时，施工队是否确认按物业错峰安排堆放
     */
    @Transactional
    public SubmitResult submit(Long registrationId, LocalDate reportDate, int bagCount, String pileLocation,
                               Long pointId, boolean wood, boolean glass, boolean ceramic, boolean household,
                               boolean extra, boolean acknowledgePeakShift) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + registrationId));
        if (reg.getStatus() != RegistrationStatus.APPROVED) {
            throw new BusinessException("装修登记未审核通过，不能上报堆放（当前 " + reg.getStatus() + "）");
        }
        if (bagCount <= 0) {
            throw new BusinessException("袋装数量必须大于0");
        }
        WastePoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new BusinessException("堆放点位不存在: " + pointId));
        LocalDate date = reportDate == null ? LocalDate.now() : reportDate;
        reportRepository.findByRegistrationIdAndReportDate(registrationId, date)
                .ifPresent(r -> {
                    throw new BusinessException(date + " 当日已上报，请使用临时加袋功能");
                });

        // —— 公共空间容量规则：接近上限不简单通过 ——
        CapacityService.CapacityCheck check = capacityService.evaluate(pointId, bagCount);
        String notice = null;
        if (check.decision() == CapacityDecision.REJECTED_FULL) {
            // 留一条违规线索（强行堆放倾向）并拒绝
            throw new BusinessException(check.message());
        }
        if (check.decision() == CapacityDecision.PEAK_SHIFT && !acknowledgePeakShift) {
            throw new BusinessException("容量管控：" + check.message()
                    + "。如确认按错峰安排堆放，请带 acknowledgePeakShift=true 重新提交；"
                    + "或请物业对点位设置临时围挡后再上报。");
        }
        if (check.decision() == CapacityDecision.PEAK_SHIFT) {
            notice = "已确认错峰堆放。" + check.message();
        }

        DailyReport report = new DailyReport();
        report.setRegistration(reg);
        report.setReportDate(date);
        report.setBagCount(bagCount);
        report.setPileLocation(pileLocation);
        report.setPointId(pointId);
        report.setContainsWood(wood);
        report.setContainsGlass(glass);
        report.setContainsCeramic(ceramic);
        report.setContainsHousehold(household);
        report.setStatus(ReportStatus.SUBMITTED);
        report = reportRepository.save(report);

        BigDecimal defaultBagKg = feeService.get(reg.getCommunityCode(),
                FeeService.DEFAULT_BAG_KG, BigDecimal.valueOf(25));
        List<WasteBag> bags = createBags(reg, report, bagCount, wood, glass, ceramic, household,
                defaultBagKg, extra, 1);

        // 堆到消防通道：立即登记违规（待物业认定）
        if (pileLocation != null && pileLocation.contains("消防通道")) {
            violationService.open(ViolationType.FIRE_LANE, ViolationSource.CREW_REPORT,
                    "施工队上报堆放位置位于消防通道：" + pileLocation,
                    reg.getId(), null, null, null, "CREW",
                    feeService.get(reg.getCommunityCode(), FeeService.FINE_FIRE_LANE, BigDecimal.valueOf(500)),
                    "施工队日报自报位置", false);
            notice = (notice == null ? "" : notice + "；") + "警告：消防通道为禁堆区域，已生成违规记录待物业认定";
        }

        traceService.record(extra ? TraceType.EXTRA_BAGS : TraceType.REPORTED, Role.CREW.name(),
                (extra ? "业主临时加袋/施工队补报" : "施工队每日上报") + "：" + date + " 在 "
                        + point.getName() + "（" + pileLocation + "）堆放 " + bagCount + " 袋"
                        + (wood ? " 含木板" : "") + (glass ? " 含玻璃" : "")
                        + (ceramic ? " 含瓷砖" : "") + (household ? " 含生活垃圾(违规)" : ""),
                reg.getId(), null, null, null, null, null, null);
        capacityService.refreshPointStatus(pointId);
        return new SubmitResult(report, bags, notice);
    }

    /** 业主临时加袋（当日已上报后追加） */
    @Transactional
    public SubmitResult addExtraBags(Long registrationId, LocalDate reportDate, int extraCount,
                                     Long pointId, boolean wood, boolean glass, boolean ceramic,
                                     boolean household, boolean acknowledgePeakShift) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new BusinessException("装修登记不存在: " + registrationId));
        LocalDate date = reportDate == null ? LocalDate.now() : reportDate;
        DailyReport report = reportRepository.findByRegistrationIdAndReportDate(registrationId, date)
                .orElseThrow(() -> new BusinessException(date + " 尚无日报，应先提交当日堆放上报"));
        if (extraCount <= 0) {
            throw new BusinessException("加袋数量必须大于0");
        }
        Long pid = pointId != null ? pointId : report.getPointId();
        CapacityService.CapacityCheck check = capacityService.evaluate(pid, extraCount);
        String notice = null;
        if (check.decision() == CapacityDecision.REJECTED_FULL) {
            throw new BusinessException(check.message());
        }
        if (check.decision() == CapacityDecision.PEAK_SHIFT && !acknowledgePeakShift) {
            throw new BusinessException("容量管控：" + check.message()
                    + "。确认错峰请带 acknowledgePeakShift=true，或由物业设置临时围挡。");
        }
        if (check.decision() == CapacityDecision.PEAK_SHIFT) {
            notice = "已确认错峰加袋。" + check.message();
        }

        int startSeq = bagRepository.findByDailyReportId(report.getId()).size() + 1;
        report.setBagCount(report.getBagCount() + extraCount);
        if (pointId != null) report.setPointId(pointId);
        if (wood) report.setContainsWood(true);
        if (glass) report.setContainsGlass(true);
        if (ceramic) report.setContainsCeramic(true);
        if (household) report.setContainsHousehold(true);
        report = reportRepository.save(report);

        BigDecimal defaultBagKg = feeService.get(reg.getCommunityCode(),
                FeeService.DEFAULT_BAG_KG, BigDecimal.valueOf(25));
        List<WasteBag> bags = createBags(reg, report, extraCount, wood, glass, ceramic, household,
                defaultBagKg, true, startSeq);

        traceService.record(TraceType.EXTRA_BAGS, Role.OWNER.name(),
                "业主临时加袋 " + extraCount + " 袋，点位 " + pid + "，当日累计 " + report.getBagCount() + " 袋",
                reg.getId(), null, null, null, null, null, null);
        publisher.publish("waste.appointment.extraBags",
                DomainEvent.of("EXTRA_BAGS", "业主临时加袋 " + extraCount)
                        .ref(reg.getId(), null, null)
                        .put("reportId", report.getId())
                        .put("extraCount", extraCount));
        capacityService.refreshPointStatus(pid);
        return new SubmitResult(report, bags, notice);
    }

    private List<WasteBag> createBags(Registration reg, DailyReport report, int count,
                                     boolean wood, boolean glass, boolean ceramic, boolean household,
                                     BigDecimal defaultKg, boolean extra, int startSeq) {
        String category = household ? "household"
                : (wood && (glass || ceramic)) ? "mixed"
                : wood ? "wood" : glass ? "glass" : ceramic ? "ceramic" : "mixed";
        List<WasteBag> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WasteBag bag = new WasteBag();
            bag.setBagCode("R" + reg.getId() + "-D" + report.getId() + "-" + (startSeq + i));
            bag.setRegistration(reg);
            bag.setDailyReport(report);
            bag.setSeqNo(startSeq + i);
            bag.setExtra(extra);
            bag.setCategory(category);
            bag.setEstimatedKg(defaultKg);
            bag.setStatus(BagStatus.REPORTED);
            result.add(bagRepository.save(bag));
        }
        return result;
    }

    /**
     * 物业审核日报：通过则验收袋并生成清运预约。
     */
    @Transactional
    public Appointment auditReport(Long reportId, boolean approved, String remark,
                                   LocalDateTime scheduledTime, LocalDateTime deadline, Long operatorId) {
        DailyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("日报不存在: " + reportId));
        Registration reg = report.getRegistration();
        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new BusinessException("日报已审核，当前状态 " + report.getStatus());
        }
        if (!approved) {
            report.setStatus(ReportStatus.REJECTED);
            report.setAuditedBy(operatorId);
            report.setAuditedAt(LocalDateTime.now());
            report.setAuditRemark(remark);
            reportRepository.save(report);
            traceService.record(TraceType.AUDITED, Role.PROPERTY.name(),
                    "物业驳回 " + report.getReportDate() + " 日报：" + remark,
                    reg.getId(), null, null, null, null, null, null);
            return null;
        }

        report.setStatus(ReportStatus.APPROVED);
        report.setAuditedBy(operatorId);
        report.setAuditedAt(LocalDateTime.now());
        report.setAuditRemark(remark);
        reportRepository.save(report);

        List<WasteBag> bags = bagRepository.findByDailyReportId(report.getId());
        BigDecimal estKg = BigDecimal.ZERO;
        for (WasteBag bag : bags) {
            bag.setStatus(BagStatus.ACCEPTED);
            bag.setAcceptedBy(operatorId);
            bag.setAcceptedAt(LocalDateTime.now());
            bagRepository.save(bag);
            if (bag.getEstimatedKg() != null) estKg = estKg.add(bag.getEstimatedKg());
        }

        // 混入生活垃圾：验收时登记违规
        if (Boolean.TRUE.equals(report.getContainsHousehold())) {
            Violation v = violationService.open(ViolationType.MIXED_HOUSEHOLD, ViolationSource.INSPECTION,
                    "物业验收发现装修垃圾中混入生活垃圾（" + report.getReportDate() + " 日报）",
                    reg.getId(), null, null, null, "CREW",
                    feeService.get(reg.getCommunityCode(), FeeService.FINE_MIXED, BigDecimal.valueOf(300)),
                    "物业现场验收", false);
            violationService.confirm(v.getId(), operatorId);
        }

        // —— 审核通过后生成清运预约 ——
        LocalDateTime scheduled = scheduledTime != null ? scheduledTime
                : LocalDate.now().atTime(LocalTime.of(18, 0));
        LocalDateTime dl = deadline != null ? deadline : scheduled.plusHours(12);
        if (!dl.isAfter(scheduled)) {
            throw new BusinessException("最晚清运时间必须晚于计划时间");
        }
        Appointment appt = new Appointment();
        appt.setApptNo(NoGenerator.next("APT"));
        appt.setRegistrationId(reg.getId());
        appt.setPointId(report.getPointId());
        appt.setBagCount(bags.size());
        appt.setEstimatedKg(estKg);
        appt.setScheduledTime(scheduled);
        appt.setDeadline(dl);
        appt.setStatus(AppointmentStatus.APPROVED);
        appt.setCreatedBy(operatorId);
        appt.setRemark(remark);
        appt = appointmentRepository.save(appt);

        for (WasteBag bag : bags) {
            appointmentBagRepository.save(new AppointmentBag(appt.getId(), bag.getId()));
            bag.setStatus(BagStatus.SCHEDULED);
            bag.setAppointmentId(appt.getId());
            bagRepository.save(bag);
        }

        capacityService.refreshPointStatus(report.getPointId());
        traceService.record(TraceType.APPOINTMENT_CREATED, Role.PROPERTY.name(),
                "物业审核通过并生成清运预约 " + appt.getApptNo() + "：点位 " + report.getPointId()
                        + "，" + bags.size() + " 袋，约 " + estKg + "kg，计划 " + scheduled
                        + "，最晚清运 " + dl,
                reg.getId(), null, appt.getId(), null, null, null, null);
        publisher.publish("waste.appointment.created",
                DomainEvent.of("APPOINTMENT_CREATED", "新清运预约 " + appt.getApptNo())
                        .ref(reg.getId(), appt.getId(), null)
                        .put("pointId", report.getPointId())
                        .put("bagCount", bags.size())
                        .put("scheduledTime", scheduled.toString()));
        return appt;
    }

    public DailyReport mustGetReport(Long id) {
        return reportRepository.findById(id).orElseThrow(() -> new BusinessException("日报不存在: " + id));
    }
}
