package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.DepositDeduction;
import com.waste.entity.RenovationRegistration;
import com.waste.enums.Frequency;
import com.waste.enums.Role;
import com.waste.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "装修登记", description = "业主装修前登记房号、施工队、工期、垃圾类型、清运频次、押金")
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    public record CreateRequest(
            @NotBlank String building,
            @NotBlank String roomNumber,
            @NotBlank String constructionTeam,
            @NotNull LocalDate expectedStartDate,
            @NotNull LocalDate expectedEndDate,
            @NotBlank String wasteTypes,
            @NotNull Frequency frequency,
            @NotNull @DecimalMin(value = "0.01", message = "押金必须大于0") BigDecimal depositAmount) {
    }

    @Operation(summary = "业主提交装修登记")
    @PostMapping
    public ApiResponse<RenovationRegistration> create(@RequestBody @Valid CreateRequest req) {
        Session owner = AuthService.require(Role.OWNER);
        return ApiResponse.ok(registrationService.create(owner, req.building(), req.roomNumber(),
                req.constructionTeam(), req.expectedStartDate(), req.expectedEndDate(),
                req.wasteTypes(), req.frequency(), req.depositAmount()));
    }

    @Operation(summary = "登记列表（业主看自己的，物业/财务看全部，可按楼栋过滤）")
    @GetMapping
    public ApiResponse<List<RenovationRegistration>> list(@RequestParam(required = false) String building) {
        return ApiResponse.ok(registrationService.list(AuthService.current(), building));
    }

    @Operation(summary = "登记详情")
    @GetMapping("/{id}")
    public ApiResponse<RenovationRegistration> get(@PathVariable Long id) {
        return ApiResponse.ok(registrationService.get(id));
    }

    @Operation(summary = "装修完工结案")
    @PutMapping("/{id}/complete")
    public ApiResponse<RenovationRegistration> complete(@PathVariable Long id) {
        Session s = AuthService.require(Role.OWNER, Role.PROPERTY);
        return ApiResponse.ok(registrationService.complete(s, id));
    }

    @Operation(summary = "押金扣款/返还流水")
    @GetMapping("/{id}/deposit-log")
    public ApiResponse<List<DepositDeduction>> depositLog(@PathVariable Long id) {
        Session s = AuthService.require(Role.OWNER, Role.PROPERTY, Role.FINANCE);
        return ApiResponse.ok(registrationService.depositLog(s, id));
    }
}
