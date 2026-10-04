package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.DisputeStatus;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.Dispute;
import com.community.waste.repository.DisputeRepository;
import com.community.waste.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "12-费用争议", description = "业主/施工队/清运公司对费用提出异议，物业与财务裁决调整")
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    public record RaiseRequest(Long billId, String raisedByRole, String raisedByName,
                               String reason, String evidence) {}
    public record ReviewRequest(Boolean approveAdjustment, BigDecimal adjustment, String result) {}

    private final DisputeService service;
    private final DisputeRepository repository;

    public DisputeController(DisputeService service, DisputeRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(summary = "提出费用争议（账单挂起，暂不核销）")
    @PostMapping
    public Dispute raise(@RequestBody RaiseRequest req) {
        CurrentUser.requireAny(Role.OWNER, Role.CREW, Role.COMPANY, Role.PROPERTY);
        return service.raise(req.billId(),
                req.raisedByRole() == null ? CurrentUser.role().name() : req.raisedByRole(),
                req.raisedByName(), req.reason(), req.evidence());
    }

    @Operation(summary = "标记核查中")
    @PostMapping("/{id}/reviewing")
    public Dispute reviewing(@PathVariable Long id) {
        CurrentUser.requireAny(Role.PROPERTY, Role.FINANCE);
        return service.underReview(id);
    }

    @Operation(summary = "裁决：批准调整（给调整金额，负数减收）或驳回维持原账")
    @PostMapping("/{id}/review")
    public Dispute review(@PathVariable Long id, @RequestBody ReviewRequest req) {
        CurrentUser.requireAny(Role.PROPERTY, Role.FINANCE);
        return service.review(id, Boolean.TRUE.equals(req.approveAdjustment()),
                req.adjustment(), req.result(), null);
    }

    @Operation(summary = "争议列表（可按状态筛选）")
    @GetMapping
    public List<Dispute> list(@RequestParam(required = false) DisputeStatus status) {
        return status == null ? repository.findAll() : repository.findByStatus(status);
    }
}
