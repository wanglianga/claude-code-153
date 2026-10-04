package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.FeeConfig;
import com.community.waste.service.FeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "15-计费参数", description = "物业/财务维护单价、罚款标准、容量系数、异常阈值")
@RestController
@RequestMapping("/api/fee-configs")
public class FeeConfigController {

    public record UpdateRequest(String key, BigDecimal value, String remark) {}

    private final FeeService feeService;

    public FeeConfigController(FeeService feeService) {
        this.feeService = feeService;
    }

    @Operation(summary = "计费参数列表")
    @GetMapping
    public List<FeeConfig> list() {
        return feeService.all();
    }

    @Operation(summary = "调整计费参数（物业/财务）")
    @PostMapping
    public FeeConfig update(@RequestBody UpdateRequest req) {
        CurrentUser.requireAny(Role.PROPERTY, Role.FINANCE);
        return feeService.set(req.key(), req.value(), req.remark());
    }
}
