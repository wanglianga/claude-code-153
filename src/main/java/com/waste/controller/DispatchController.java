package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.DispatchOrder;
import com.waste.enums.Role;
import com.waste.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "清运派单", description = "清运公司按门禁、车辆限行、核载容量、已有派单安排车辆")
@RestController
@RequestMapping("/api/dispatches")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    public record CreateRequest(
            @NotNull Long appointmentId,
            @NotNull Long vehicleId,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedArrival,
            String routeInfo) {
    }

    public record ArriveRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualArrival) {
    }

    @Operation(summary = "创建派单（自动校验限行/核载/时段冲突，生成门禁放行单号）")
    @PostMapping
    public ApiResponse<DispatchOrder> create(@RequestBody @Valid CreateRequest req) {
        Session collector = AuthService.require(Role.COLLECTOR);
        return ApiResponse.ok(dispatchService.create(collector, req.appointmentId(), req.vehicleId(),
                req.plannedArrival(), req.routeInfo()));
    }

    @Operation(summary = "司机到场登记（判定迟到，迟到且垃圾点超容量时自动生成溢出投诉）")
    @PostMapping("/{id}/arrive")
    public ApiResponse<DispatchOrder> arrive(@PathVariable Long id, @RequestBody(required = false) ArriveRequest req) {
        Session collector = AuthService.require(Role.COLLECTOR);
        return ApiResponse.ok(dispatchService.arrive(collector, id, req == null ? null : req.actualArrival()));
    }

    @Operation(summary = "派单列表（清运公司看本公司，物业看全部）")
    @GetMapping
    public ApiResponse<List<DispatchOrder>> list() {
        return ApiResponse.ok(dispatchService.list(AuthService.current()));
    }

    @Operation(summary = "派单详情")
    @GetMapping("/{id}")
    public ApiResponse<DispatchOrder> get(@PathVariable Long id) {
        return ApiResponse.ok(dispatchService.get(id));
    }
}
