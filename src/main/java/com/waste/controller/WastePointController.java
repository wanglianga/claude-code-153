package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.WastePoint;
import com.waste.enums.Role;
import com.waste.service.WastePointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "垃圾点", description = "小区装修垃圾堆放点容量管理，支持临时围挡扩容")
@RestController
@RequestMapping("/api/waste-points")
@RequiredArgsConstructor
public class WastePointController {

    private final WastePointService wastePointService;

    public record CreateRequest(@NotBlank String name, @NotBlank String location, @Min(1) int capacityBags) {
    }

    public record FenceRequest(boolean enable, @Min(0) int extraCapacity) {
    }

    @Operation(summary = "垃圾点列表（含当前堆放量/容量/围挡状态）")
    @GetMapping
    public ApiResponse<List<WastePoint>> list() {
        return ApiResponse.ok(wastePointService.list());
    }

    @Operation(summary = "新增垃圾点（物业）")
    @PostMapping
    public ApiResponse<WastePoint> create(@RequestBody @Valid CreateRequest req) {
        Session property = AuthService.require(Role.PROPERTY);
        return ApiResponse.ok(wastePointService.create(property, req.name(), req.location(), req.capacityBags()));
    }

    @Operation(summary = "安排/撤除临时围挡（物业）")
    @PostMapping("/{id}/fence")
    public ApiResponse<WastePoint> fence(@PathVariable Long id, @RequestBody @Valid FenceRequest req) {
        Session property = AuthService.require(Role.PROPERTY);
        return ApiResponse.ok(wastePointService.setFence(property, id, req.enable(), req.extraCapacity()));
    }
}
