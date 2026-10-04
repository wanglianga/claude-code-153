package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.Registration;
import com.community.waste.repository.RegistrationRepository;
import com.community.waste.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "1-装修登记", description = "业主在物业登记房号/施工队/工期/垃圾类型/清运频次/押金，物业审核")
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    public record RegisterRequest(
            @NotBlank String communityCode,
            @NotBlank String communityName,
            @NotBlank String buildingNo,
            @NotBlank String roomNo,
            @NotBlank String ownerName,
            String ownerPhone,
            @NotBlank String crewName,
            String crewContact,
            String crewPhone,
            @NotNull LocalDate startDate,
            @NotNull LocalDate expectedEndDate,
            @NotBlank String wasteTypes,
            @NotBlank String haulFrequency,
            @NotNull BigDecimal deposit) {}

    public record AuditRequest(boolean approved, String reason) {}

    private final RegistrationService service;
    private final RegistrationRepository repository;

    public RegistrationController(RegistrationService service, RegistrationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(summary = "提交装修登记（物业/业主）")
    @PostMapping
    public Registration register(@Valid @RequestBody RegisterRequest req) {
        CurrentUser.requireAny(Role.PROPERTY, Role.OWNER);
        Registration r = new Registration();
        r.setCommunityCode(req.communityCode());
        r.setCommunityName(req.communityName());
        r.setBuildingNo(req.buildingNo());
        r.setRoomNo(req.roomNo());
        r.setOwnerName(req.ownerName());
        r.setOwnerPhone(req.ownerPhone());
        r.setCrewName(req.crewName());
        r.setCrewContact(req.crewContact());
        r.setCrewPhone(req.crewPhone());
        r.setStartDate(req.startDate());
        r.setExpectedEndDate(req.expectedEndDate());
        r.setWasteTypes(req.wasteTypes());
        r.setHaulFrequency(req.haulFrequency());
        r.setDeposit(req.deposit());
        return service.register(r);
    }

    @Operation(summary = "物业审核通过")
    @PostMapping("/{id}/approve")
    public Registration approve(@PathVariable Long id) {
        CurrentUser.require(Role.PROPERTY);
        return service.approve(id, null);
    }

    @Operation(summary = "物业驳回")
    @PostMapping("/{id}/reject")
    public Registration reject(@PathVariable Long id, @RequestBody AuditRequest req) {
        CurrentUser.require(Role.PROPERTY);
        return service.reject(id, req.reason());
    }

    @Operation(summary = "物业关闭登记（装修结束）")
    @PostMapping("/{id}/close")
    public Registration close(@PathVariable Long id) {
        CurrentUser.require(Role.PROPERTY);
        return service.close(id);
    }

    @Operation(summary = "登记详情")
    @GetMapping("/{id}")
    public Registration get(@PathVariable Long id) {
        return service.mustGet(id);
    }

    @Operation(summary = "登记列表（可按楼栋筛选）")
    @GetMapping
    public List<Registration> list(@RequestParam(required = false) String buildingNo) {
        return buildingNo == null ? repository.findAll() : repository.findByBuildingNo(buildingNo);
    }
}
