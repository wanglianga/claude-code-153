package com.community.waste.web;

import com.community.waste.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "13-全链路追溯", description = "同一袋垃圾从登记/上报/审核/预约/装车/称重/处置回执/费用结算全程还原，投诉时追责到人")
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TrackingService trackingService;

    public TraceController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Operation(summary = "按垃圾袋编码还原全链路（谁堆放、谁审核、谁清走、车牌司机、称重、回执、费用）")
    @GetMapping("/bag/{bagCode}")
    public Map<String, Object> byBag(@PathVariable String bagCode) {
        return trackingService.traceBag(bagCode);
    }

    @Operation(summary = "按装修登记（房号）还原全部袋、预约、账单、违规、投诉、押金扣款")
    @GetMapping("/registration/{registrationId}")
    public Map<String, Object> byRegistration(@PathVariable Long registrationId) {
        return trackingService.traceRegistration(registrationId);
    }

    @Operation(summary = "投诉追责视图：投诉及其关联房号的完整责任链")
    @GetMapping("/complaint/{complaintId}")
    public Map<String, Object> byComplaint(@PathVariable Long complaintId) {
        return trackingService.accountabilityForComplaint(complaintId);
    }
}
