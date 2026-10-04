package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.WeighRecord;
import com.waste.enums.Role;
import com.waste.service.WeighService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "称重", description = "车辆进出场称重：车牌、司机、空车/装载重量、现场照片、清运联单；偏差超限自动标记异常")
@RestController
@RequestMapping("/api/weigh-records")
@RequiredArgsConstructor
public class WeighController {

    private final WeighService weighService;

    public record CreateRequest(
            @NotNull Long dispatchId,
            @NotNull BigDecimal emptyWeightKg,
            @NotNull BigDecimal loadedWeightKg,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entryTime,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime exitTime,
            String photos,
            String manifestNo) {
    }

    @Operation(summary = "提交进出场称重记录（自动触发计费）")
    @PostMapping
    public ApiResponse<WeighRecord> create(@RequestBody @Valid CreateRequest req) {
        Session collector = AuthService.require(Role.COLLECTOR);
        return ApiResponse.ok(weighService.create(collector, req.dispatchId(), req.emptyWeightKg(),
                req.loadedWeightKg(), req.entryTime(), req.exitTime(), req.photos(), req.manifestNo()));
    }

    @Operation(summary = "称重记录列表")
    @GetMapping
    public ApiResponse<List<WeighRecord>> list() {
        AuthService.require(Role.COLLECTOR, Role.PROPERTY, Role.FINANCE);
        return ApiResponse.ok(weighService.list());
    }

    @Operation(summary = "称重记录详情")
    @GetMapping("/{id}")
    public ApiResponse<WeighRecord> get(@PathVariable Long id) {
        AuthService.require(Role.COLLECTOR, Role.PROPERTY, Role.FINANCE);
        return ApiResponse.ok(weighService.get(id));
    }
}
