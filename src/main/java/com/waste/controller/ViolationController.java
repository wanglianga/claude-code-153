package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.Violation;
import com.waste.enums.Role;
import com.waste.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "违规管理", description = "消防通道堆放、混入生活垃圾等违规记录与押金扣款")
@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;

    @Operation(summary = "违规列表")
    @GetMapping
    public ApiResponse<List<Violation>> list(@RequestParam(required = false) Long registrationId) {
        AuthService.require(Role.PROPERTY, Role.FINANCE);
        return ApiResponse.ok(violationService.list(registrationId));
    }

    @Operation(summary = "豁免违规（物业，仅限未扣款）")
    @PostMapping("/{id}/waive")
    public ApiResponse<Violation> waive(@PathVariable Long id) {
        Session property = AuthService.require(Role.PROPERTY);
        return ApiResponse.ok(violationService.waive(id, property));
    }
}
