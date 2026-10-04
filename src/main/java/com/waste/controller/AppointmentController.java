package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.CollectionAppointment;
import com.waste.enums.ApptStatus;
import com.waste.enums.Role;
import com.waste.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "清运预约", description = "物业审核上报单生成清运预约；垃圾点接近容量上限时拒绝直接通过并提示错峰/围挡")
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    public record ReviewRequest(
            @NotNull Long reportId,
            boolean approve,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate apptDate,
            @NotNull @Pattern(regexp = "AM|PM", message = "时段只能是 AM 或 PM") String timeSlot,
            String comment) {
    }

    @Operation(summary = "物业审核上报单（容量不足时返回 REJECTED_CAPACITY 及错峰/围挡提示）")
    @PostMapping("/review")
    public ApiResponse<CollectionAppointment> review(@RequestBody @Valid ReviewRequest req) {
        Session property = AuthService.require(Role.PROPERTY);
        return ApiResponse.ok(appointmentService.review(property, req.reportId(), req.approve(),
                req.apptDate(), req.timeSlot(), req.comment() == null ? "" : req.comment()));
    }

    @Operation(summary = "预约列表（业主/施工队看自己，物业/清运/财务看全部）")
    @GetMapping
    public ApiResponse<List<CollectionAppointment>> list(@RequestParam(required = false) ApptStatus status) {
        return ApiResponse.ok(appointmentService.list(AuthService.current(), status));
    }

    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public ApiResponse<CollectionAppointment> get(@PathVariable Long id) {
        return ApiResponse.ok(appointmentService.get(id));
    }
}
