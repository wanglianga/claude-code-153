package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.Bill;
import com.waste.enums.BillStatus;
import com.waste.enums.BillType;
import com.waste.enums.Role;
import com.waste.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "账单结算", description = "财务端：业主/施工队/物业垫付/清运公司四类账单的查询、收款、争议调整")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillingService billingService;

    public record AdjustRequest(@NotNull BigDecimal newAmount, @NotBlank String reason) {
    }

    @Operation(summary = "账单列表（按类型/状态/账期过滤）")
    @GetMapping
    public ApiResponse<List<Bill>> list(@RequestParam(required = false) BillType type,
                                        @RequestParam(required = false) BillStatus status,
                                        @RequestParam(required = false) String period) {
        AuthService.require(Role.FINANCE, Role.PROPERTY);
        return ApiResponse.ok(billingService.list(type, status, period));
    }

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    public ApiResponse<Bill> get(@PathVariable Long id) {
        AuthService.require(Role.FINANCE, Role.PROPERTY);
        return ApiResponse.ok(billingService.get(id));
    }

    @Operation(summary = "确认结清（施工队缴罚款自动返还业主押金）")
    @PostMapping("/{id}/pay")
    public ApiResponse<Bill> pay(@PathVariable Long id) {
        Session finance = AuthService.require(Role.FINANCE);
        return ApiResponse.ok(billingService.pay(id, finance));
    }

    @Operation(summary = "费用争议调整（仅争议中账单，差额自动回冲/补扣押金）")
    @PostMapping("/{id}/adjust")
    public ApiResponse<Bill> adjust(@PathVariable Long id, @RequestBody @Valid AdjustRequest req) {
        Session finance = AuthService.require(Role.FINANCE);
        return ApiResponse.ok(billingService.adjust(id, req.newAmount(), req.reason(), finance));
    }
}
