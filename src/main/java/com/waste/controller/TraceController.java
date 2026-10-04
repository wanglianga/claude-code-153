package com.waste.controller;

import com.waste.auth.AuthService;
import com.waste.common.ApiResponse;
import com.waste.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "全链路追溯", description = "还原谁堆放、谁审核、谁清走、费用如何产生")
@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @Operation(summary = "按预约 ID 追溯全链路（登记→上报→审核→派单→称重→处置→账单→事件流）")
    @GetMapping("/appointments/{id}")
    public ApiResponse<Map<String, Object>> byAppointment(@PathVariable Long id) {
        return ApiResponse.ok(traceService.traceByAppointment(AuthService.current(), id));
    }

    @Operation(summary = "按追溯码查询")
    @GetMapping("/code/{traceCode}")
    public ApiResponse<Map<String, Object>> byCode(@PathVariable String traceCode) {
        return ApiResponse.ok(traceService.traceByCode(AuthService.current(), traceCode));
    }
}
