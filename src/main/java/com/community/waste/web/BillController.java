package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.BillPartyType;
import com.community.waste.domain.DomainEnums.BillStatus;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.Bill;
import com.community.waste.repository.BillRepository;
import com.community.waste.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "10-财务账单", description = "按业主/施工队/物业垫付/清运公司结算的账单、核销与汇总")
@RestController
@RequestMapping("/api/bills")
public class BillController {

    public record PayRequest(String channel) {}

    private final BillRepository repository;
    private final BillingService billingService;

    public BillController(BillRepository repository, BillingService billingService) {
        this.repository = repository;
        this.billingService = billingService;
    }

    @Operation(summary = "账单列表（可按状态/结算对象类型/装修登记筛选）")
    @GetMapping
    public List<Bill> list(@RequestParam(required = false) BillStatus status,
                           @RequestParam(required = false) BillPartyType partyType,
                           @RequestParam(required = false) Long registrationId) {
        return repository.findAll().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> partyType == null || b.getPartyType() == partyType)
                .filter(b -> registrationId == null || registrationId.equals(b.getRegistrationId()))
                .toList();
    }

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    public Bill get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Operation(summary = "账单收款/付款核销（财务）")
    @PostMapping("/{id}/pay")
    public Bill pay(@PathVariable Long id, @RequestBody(required = false) PayRequest req) {
        CurrentUser.require(Role.FINANCE);
        return billingService.markPaid(id, req == null ? null : req.channel());
    }
}
