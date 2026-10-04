package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.ComplaintStatus;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.domain.DomainEnums.ViolationType;
import com.community.waste.entity.Complaint;
import com.community.waste.repository.ComplaintRepository;
import com.community.waste.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "9-邻居投诉", description = "异味/扬尘/噪音/违规堆放等投诉受理、核查、追责闭环")
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    public record ResolveRequest(String result, String liableParty, Boolean openViolation,
                                 ViolationType violationType, BigDecimal fineAmount,
                                 Boolean confirmNow) {}

    private final ComplaintService service;
    private final ComplaintRepository repository;

    public ComplaintController(ComplaintService service, ComplaintRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(summary = "提交投诉（邻居/业主/物业均可登记）")
    @PostMapping
    public Complaint submit(@RequestBody Complaint complaint) {
        return service.submit(complaint);
    }

    @Operation(summary = "物业受理并开始核查")
    @PostMapping("/{id}/investigate")
    public Complaint investigate(@PathVariable Long id) {
        CurrentUser.require(Role.PROPERTY);
        return service.investigate(id, null);
    }

    @Operation(summary = "处理投诉：可同时认定违规责任方并罚款（openViolation/confirmNow）")
    @PostMapping("/{id}/resolve")
    public Complaint resolve(@PathVariable Long id, @RequestBody ResolveRequest req) {
        CurrentUser.require(Role.PROPERTY);
        return service.resolve(id, req.result(), req.liableParty(),
                Boolean.TRUE.equals(req.openViolation()), req.violationType(), req.fineAmount(),
                Boolean.TRUE.equals(req.confirmNow()), null);
    }

    @Operation(summary = "驳回投诉")
    @PostMapping("/{id}/reject")
    public Complaint reject(@PathVariable Long id, @RequestBody ResolveRequest req) {
        CurrentUser.require(Role.PROPERTY);
        return service.reject(id, req.result(), null);
    }

    @Operation(summary = "投诉列表（可按状态筛选）")
    @GetMapping
    public List<Complaint> list(@RequestParam(required = false) ComplaintStatus status) {
        return status == null ? repository.findAll() : repository.findByStatus(status);
    }
}
