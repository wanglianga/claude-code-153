package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.domain.DomainEnums.ViolationSource;
import com.community.waste.domain.DomainEnums.ViolationType;
import com.community.waste.entity.Violation;
import com.community.waste.repository.ViolationRepository;
import com.community.waste.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "8-违规追责", description = "消防通道堆放/混入生活垃圾/违规堆放/迟到溢出等违规登记、物业认定与扣款")
@RestController
@RequestMapping("/api/violations")
public class ViolationController {

    public record OpenRequest(
            @NotNull ViolationType type,
            ViolationSource source,
            @NotBlank String description,
            Long registrationId,
            Long appointmentId,
            Long tripId,
            Long complaintId,
            String liableParty,
            BigDecimal fineAmount,
            String evidence,
            Boolean autoConfirm) {}

    public record RejectRequest(String reason) {}

    private final ViolationService service;
    private final ViolationRepository repository;

    public ViolationController(ViolationService service, ViolationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(summary = "登记违规（物业巡检/系统/投诉来源），autoConfirm=true 时物业直接认定并扣款")
    @PostMapping
    public Violation open(@RequestBody OpenRequest req) {
        CurrentUser.requireAny(Role.PROPERTY, Role.COMPANY);
        Violation v = service.open(req.type(),
                req.source() == null ? ViolationSource.INSPECTION : req.source(),
                req.description(), req.registrationId(), req.appointmentId(), req.tripId(),
                req.complaintId(), req.liableParty(), req.fineAmount(), req.evidence(), false);
        if (Boolean.TRUE.equals(req.autoConfirm())) {
            v = service.confirm(v.getId(), null);
        }
        return v;
    }

    @Operation(summary = "物业认定违规成立（执行押金扣款/开罚款单）")
    @PostMapping("/{id}/confirm")
    public Violation confirm(@PathVariable Long id) {
        CurrentUser.require(Role.PROPERTY);
        return service.confirm(id, null);
    }

    @Operation(summary = "物业驳回违规")
    @PostMapping("/{id}/reject")
    public Violation reject(@PathVariable Long id, @RequestBody RejectRequest req) {
        CurrentUser.require(Role.PROPERTY);
        return service.reject(id, req.reason());
    }

    @Operation(summary = "违规列表（可按装修登记筛选）")
    @GetMapping
    public List<Violation> list(@RequestParam(required = false) Long registrationId) {
        return registrationId == null ? repository.findAll() : repository.findByRegistrationId(registrationId);
    }
}
