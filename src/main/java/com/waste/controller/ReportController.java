package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.DailyReport;
import com.waste.enums.ReportStatus;
import com.waste.enums.Role;
import com.waste.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "每日上报", description = "施工队每天提交袋装数量、堆放位置、分类信息；支持业主临时加袋")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    public record SubmitRequest(
            @NotNull Long registrationId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @Min(1) int bagCount,
            @Min(0) int extraBagCount,
            @NotBlank String stackingLocation,
            @NotNull Long wastePointId,
            boolean containsWood,
            boolean containsGlass,
            boolean containsTile,
            boolean containsMixedHousehold) {
    }

    public record ExtraBagsRequest(@Min(1) int extraCount) {
    }

    @Operation(summary = "施工队提交每日袋装上报（自动检测消防通道堆放/混入生活垃圾违规）")
    @PostMapping
    public ApiResponse<DailyReport> submit(@RequestBody @Valid SubmitRequest req) {
        Session worker = AuthService.require(Role.WORKER);
        return ApiResponse.ok(reportService.submit(worker, req.registrationId(), req.reportDate(),
                req.bagCount(), req.extraBagCount(), req.stackingLocation(), req.wastePointId(),
                req.containsWood(), req.containsGlass(), req.containsTile(), req.containsMixedHousehold()));
    }

    @Operation(summary = "临时加袋（业主/施工队，仅限待审核上报单）")
    @PostMapping("/{id}/extra-bags")
    public ApiResponse<DailyReport> extraBags(@PathVariable Long id, @RequestBody @Valid ExtraBagsRequest req) {
        Session s = AuthService.require(Role.OWNER, Role.WORKER);
        return ApiResponse.ok(reportService.addExtraBags(s, id, req.extraCount()));
    }

    @Operation(summary = "上报单列表")
    @GetMapping
    public ApiResponse<List<DailyReport>> list(@RequestParam(required = false) Long registrationId,
                                               @RequestParam(required = false) ReportStatus status) {
        return ApiResponse.ok(reportService.list(AuthService.current(), registrationId, status));
    }
}
