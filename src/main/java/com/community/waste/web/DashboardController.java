package com.community.waste.web;

import com.community.waste.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "14-看板", description = "物业端（楼栋预约/超时未清/违规堆放/押金扣款/公司履约）、清运公司端（派单/容量/称重/到场）、财务端汇总")
@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(summary = "物业端总览")
    @GetMapping("/property")
    public Map<String, Object> property() {
        return service.propertyOverview();
    }

    @Operation(summary = "清运公司端看板：车辆活跃派单、路线、待派单预约")
    @GetMapping("/company")
    public Map<String, Object> company() {
        return service.companyBoard();
    }

    @Operation(summary = "清运公司履约统计")
    @GetMapping("/company/performance")
    public Map<String, Object> performance() {
        return service.companyPerformance();
    }

    @Operation(summary = "财务端账单总览（四方应收应付）")
    @GetMapping("/finance")
    public Map<String, Object> finance() {
        return service.financeOverview();
    }
}
