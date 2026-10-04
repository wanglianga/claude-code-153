package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.DisposalReceipt;
import com.community.waste.service.DisposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Tag(name = "7-处置场回执", description = "处置场复称、消纳费、照片、回执编号，差异检测，袋与预约闭环")
@RestController
@RequestMapping("/api/disposal")
public class DisposalController {

    public record ReceiptRequest(String siteName, BigDecimal receivedWeight, BigDecimal unitPrice,
                                 LocalDateTime receivedAt, String operator, String photoUrl, String receiptNo) {}

    private final DisposalService disposalService;

    public DisposalController(DisposalService disposalService) {
        this.disposalService = disposalService;
    }

    @Operation(summary = "登记处置场回执（清运公司回传，物业垫付处置费台账同步生成）")
    @PostMapping("/trips/{tripId}/receipt")
    public DisposalReceipt receive(@PathVariable Long tripId, @RequestBody ReceiptRequest req) {
        CurrentUser.requireAny(Role.COMPANY, Role.PROPERTY);
        return disposalService.receive(tripId, req.siteName(), req.receivedWeight(), req.unitPrice(),
                req.receivedAt(), req.operator(), req.photoUrl(), req.receiptNo());
    }
}
