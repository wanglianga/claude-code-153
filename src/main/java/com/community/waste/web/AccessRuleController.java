package com.community.waste.web;

import com.community.waste.entity.AccessRule;
import com.community.waste.repository.AccessRuleRepository;
import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "16-门禁限行规则", description = "小区门禁进场时间窗与按星期的尾号限行维护")
@RestController
@RequestMapping("/api/access-rules")
public class AccessRuleController {

    private final AccessRuleRepository repository;

    public AccessRuleController(AccessRuleRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "规则列表")
    @GetMapping
    public List<AccessRule> list() {
        return repository.findAll();
    }

    @Operation(summary = "新增门禁/限行规则（物业）")
    @PostMapping
    public AccessRule create(@RequestBody AccessRule rule) {
        CurrentUser.require(Role.PROPERTY);
        return repository.save(rule);
    }

    @Operation(summary = "停用规则（物业）")
    @PostMapping("/{id}/disable")
    public AccessRule disable(@PathVariable Long id) {
        CurrentUser.require(Role.PROPERTY);
        AccessRule r = repository.findById(id).orElseThrow();
        r.setEnabled(false);
        return repository.save(r);
    }
}
