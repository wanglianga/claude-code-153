package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.auth.Session;
import com.waste.common.ApiResponse;
import com.waste.entity.Complaint;
import com.waste.enums.ComplaintStatus;
import com.waste.enums.ComplaintType;
import com.waste.enums.Role;
import com.waste.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "投诉", description = "邻居/业主投诉与物业处理；费用争议自动冻结账单")
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    public record CreateRequest(
            @NotNull ComplaintType type,
            Long targetAppointmentId,
            Long targetRegistrationId,
            Long billId,
            @NotBlank String description) {
    }

    public record HandleRequest(@NotNull ComplaintStatus action, @NotBlank String resolution) {
    }

    @Operation(summary = "发起投诉（费用争议需传 billId，账单自动转为争议状态）")
    @PostMapping
    public ApiResponse<Complaint> create(@RequestBody @Valid CreateRequest req) {
        Session user = AuthService.current();
        return ApiResponse.ok(complaintService.create(user, req.type(), req.targetAppointmentId(),
                req.targetRegistrationId(), req.billId(), req.description()));
    }

    @Operation(summary = "物业处理投诉")
    @PutMapping("/{id}/handle")
    public ApiResponse<Complaint> handle(@PathVariable Long id, @RequestBody @Valid HandleRequest req) {
        Session property = AuthService.require(Role.PROPERTY);
        return ApiResponse.ok(complaintService.handle(property, id, req.action(), req.resolution()));
    }

    @Operation(summary = "投诉列表（物业/财务看全部，其他人看自己发起的）")
    @GetMapping
    public ApiResponse<List<Complaint>> list(@RequestParam(required = false) ComplaintStatus status) {
        return ApiResponse.ok(complaintService.list(AuthService.current(), status));
    }
}
