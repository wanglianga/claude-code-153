package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.Appointment;
import com.community.waste.entity.DailyReport;
import com.community.waste.entity.WasteBag;
import com.community.waste.repository.DailyReportRepository;
import com.community.waste.repository.WasteBagRepository;
import com.community.waste.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "3-每日上报与审核", description = "施工队日报（袋装数量/堆放位置/木板玻璃瓷砖分类）、物业审核生成清运预约、业主临时加袋")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    public record SubmitRequest(
            @NotNull Long registrationId,
            LocalDate reportDate,
            @NotNull Integer bagCount,
            @NotBlank String pileLocation,
            @NotNull Long pointId,
            Boolean wood, Boolean glass, Boolean ceramic, Boolean household,
            Boolean extra, Boolean acknowledgePeakShift) {}

    public record ExtraBagsRequest(
            @NotNull Long registrationId,
            LocalDate reportDate,
            @NotNull Integer extraCount,
            Long pointId,
            Boolean wood, Boolean glass, Boolean ceramic, Boolean household,
            Boolean acknowledgePeakShift) {}

    public record AuditRequest(
            @NotNull Boolean approved,
            String remark,
            LocalDateTime scheduledTime,
            LocalDateTime deadline) {}

    private final ReportService reportService;
    private final DailyReportRepository reportRepository;
    private final WasteBagRepository bagRepository;

    public ReportController(ReportService reportService, DailyReportRepository reportRepository,
                            WasteBagRepository bagRepository) {
        this.reportService = reportService;
        this.reportRepository = reportRepository;
        this.bagRepository = bagRepository;
    }

    @Operation(summary = "施工队提交当日堆放上报（接近容量上限须 acknowledgePeakShift=true 确认错峰）")
    @PostMapping
    public Map<String, Object> submit(@RequestBody SubmitRequest req) {
        CurrentUser.requireAny(Role.CREW, Role.PROPERTY, Role.OWNER);
        ReportService.SubmitResult r = reportService.submit(req.registrationId(), req.reportDate(),
                req.bagCount(), req.pileLocation(), req.pointId(),
                bool(req.wood()), bool(req.glass()), bool(req.ceramic()), bool(req.household()),
                bool(req.extra()), bool(req.acknowledgePeakShift()));
        return Map.of("report", r.report(), "bags", r.bags(),
                "notice", r.notice() == null ? "" : r.notice());
    }

    @Operation(summary = "业主临时加袋（当日已上报后追加，受容量规则约束）")
    @PostMapping("/extra-bags")
    public Map<String, Object> extra(@RequestBody ExtraBagsRequest req) {
        CurrentUser.requireAny(Role.OWNER, Role.CREW, Role.PROPERTY);
        ReportService.SubmitResult r = reportService.addExtraBags(req.registrationId(), req.reportDate(),
                req.extraCount(), req.pointId(), bool(req.wood()), bool(req.glass()),
                bool(req.ceramic()), bool(req.household()), bool(req.acknowledgePeakShift()));
        return Map.of("report", r.report(), "bags", r.bags(),
                "notice", r.notice() == null ? "" : r.notice());
    }

    @Operation(summary = "物业审核日报：通过则验收袋并生成清运预约")
    @PostMapping("/{id}/audit")
    public Appointment audit(@PathVariable Long id, @RequestBody AuditRequest req) {
        CurrentUser.require(Role.PROPERTY);
        return reportService.auditReport(id, req.approved(), req.remark(),
                req.scheduledTime(), req.deadline(), null);
    }

    @Operation(summary = "日报详情")
    @GetMapping("/{id}")
    public DailyReport get(@PathVariable Long id) {
        return reportService.mustGetReport(id);
    }

    @Operation(summary = "某装修登记的日报列表")
    @GetMapping
    public List<DailyReport> list(@RequestParam Long registrationId) {
        return reportRepository.findByRegistrationIdOrderByReportDateDesc(registrationId);
    }

    @Operation(summary = "日报下的垃圾袋（含单袋状态）")
    @GetMapping("/{id}/bags")
    public List<WasteBag> bags(@PathVariable Long id) {
        return bagRepository.findByDailyReportId(id);
    }

    private static boolean bool(Boolean b) {
        return Boolean.TRUE.equals(b);
    }
}
