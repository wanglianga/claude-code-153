package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.DepositDeduction;
import com.community.waste.entity.Registration;
import com.community.waste.service.DashboardService;
import com.community.waste.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "11-押金管理", description = "押金余额、补缴、人工扣款与扣款流水")
@RestController
@RequestMapping("/api/deposits")
public class DepositController {

    public record TopUpRequest(Long registrationId, BigDecimal amount) {}
    public record DeductRequest(Long registrationId, BigDecimal amount, String reason) {}

    private final DepositService depositService;
    private final DashboardService dashboardService;

    public DepositController(DepositService depositService, DashboardService dashboardService) {
        this.depositService = depositService;
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "查询押金可扣余额")
    @GetMapping("/balance/{registrationId}")
    public Map<String, Object> balance(@PathVariable Long registrationId) {
        return Map.of("registrationId", registrationId, "balance", depositService.balance(registrationId));
    }

    @Operation(summary = "押金补缴（业主）")
    @PostMapping("/topup")
    public Registration topUp(@RequestBody TopUpRequest req) {
        CurrentUser.requireAny(Role.OWNER, Role.PROPERTY, Role.FINANCE);
        return depositService.topUp(req.registrationId(), req.amount());
    }

    @Operation(summary = "物业人工扣款")
    @PostMapping("/deduct")
    public DepositDeduction deduct(@RequestBody DeductRequest req) {
        CurrentUser.requireAny(Role.PROPERTY, Role.FINANCE);
        return depositService.manualDeduct(req.registrationId(), req.amount(), req.reason(), null);
    }

    @Operation(summary = "押金扣款流水（可按装修登记筛选）")
    @GetMapping("/deductions")
    public List<Map<String, Object>> deductions(@RequestParam(required = false) Long registrationId) {
        return dashboardService.depositDeductions(registrationId);
    }
}
