package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.enums.Role;
import com.waste.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "看板", description = "物业端 / 清运公司端 / 财务端数据看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "物业端：各楼栋预约、超时未清、违规堆放、押金扣款、清运公司履约")
    @GetMapping("/property")
    public ApiResponse<Map<String, Object>> property() {
        AuthService.require(Role.PROPERTY);
        return ApiResponse.ok(dashboardService.property());
    }

    @Operation(summary = "清运公司端：派单路线、车辆容量、称重回传、司机到场")
    @GetMapping("/collector")
    public ApiResponse<Map<String, Object>> collector() {
        Session s = AuthService.require(Role.COLLECTOR);
        return ApiResponse.ok(dashboardService.collector(s));
    }

    @Operation(summary = "财务端：业主/施工队/物业垫付/清运公司四类账单汇总")
    @GetMapping("/finance")
    public ApiResponse<Map<String, Object>> finance(@RequestParam(required = false) String period) {
        AuthService.require(Role.FINANCE);
        return ApiResponse.ok(dashboardService.finance(period));
    }
}
