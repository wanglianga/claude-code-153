package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.DisposalReceipt;
import com.waste.enums.Role;
import com.waste.service.DisposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "处置回执", description = "处置场接收回执，完成袋级追溯的末端环节")
@RestController
@RequestMapping("/api/disposal-receipts")
@RequiredArgsConstructor
public class DisposalController {

    private final DisposalService disposalService;

    public record CreateRequest(
            @NotNull Long weighRecordId,
            @NotBlank String disposalSite,
            String receiptNo,
            @NotNull BigDecimal receivedWeightKg,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime receivedAt) {
    }

    @Operation(summary = "上传处置场回执")
    @PostMapping
    public ApiResponse<DisposalReceipt> create(@RequestBody @Valid CreateRequest req) {
        Session collector = AuthService.require(Role.COLLECTOR);
        return ApiResponse.ok(disposalService.create(collector, req.weighRecordId(), req.disposalSite(),
                req.receiptNo(), req.receivedWeightKg(), req.receivedAt()));
    }

    @Operation(summary = "回执列表")
    @GetMapping
    public ApiResponse<List<DisposalReceipt>> list() {
        AuthService.require(Role.COLLECTOR, Role.PROPERTY, Role.FINANCE);
        return ApiResponse.ok(disposalService.list());
    }
}
