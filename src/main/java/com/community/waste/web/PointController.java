package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.WastePoint;
import com.community.waste.service.CapacityService;
import com.community.waste.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "2-垃圾点容量", description = "公共空间容量纳入规则：接近上限提示错峰、物业可设临时围挡扩容")
@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;
    private final CapacityService capacityService;

    public PointController(PointService pointService, CapacityService capacityService) {
        this.pointService = pointService;
        this.capacityService = capacityService;
    }

    public record PointRequest(String communityCode, String code, String name, String location,
                               Integer capacityBags, java.math.BigDecimal capacityKg, Double nearFullThreshold) {}

    public record EnclosureRequest(LocalDateTime until, String reason) {}

    public record CapacityQuery(Long pointId, Integer newBags) {}

    @Operation(summary = "新建垃圾点（物业）")
    @PostMapping
    public WastePoint create(@RequestBody PointRequest req) {
        CurrentUser.require(Role.PROPERTY);
        WastePoint p = new WastePoint();
        if (req.communityCode() != null) p.setCommunityCode(req.communityCode());
        p.setCode(req.code());
        p.setName(req.name());
        p.setLocation(req.location());
        p.setCapacityBags(req.capacityBags());
        p.setCapacityKg(req.capacityKg());
        if (req.nearFullThreshold() != null) p.setNearFullThreshold(req.nearFullThreshold());
        return pointService.create(p);
    }

    @Operation(summary = "垃圾点列表（含实时占用/状态）")
    @GetMapping
    public List<WastePoint> list() {
        return pointService.all();
    }

    @Operation(summary = "垃圾点容量详情：占用袋数、有效容量、使用率、是否围挡中")
    @GetMapping("/{id}")
    public PointService.PointView view(@PathVariable Long id) {
        return pointService.view(id);
    }

    @Operation(summary = "容量试算：施工队上报前预判，返回 通过/错峰/拒绝 及提示语")
    @PostMapping("/capacity-check")
    public CapacityService.CapacityCheck check(@RequestBody CapacityQuery q) {
        return capacityService.evaluate(q.pointId(), q.newBags() == null ? 1 : q.newBags());
    }

    @Operation(summary = "物业设置临时围挡（围挡期内按扩容系数承接）")
    @PostMapping("/{id}/enclosure")
    public WastePoint enclosure(@PathVariable Long id, @RequestBody EnclosureRequest req) {
        CurrentUser.require(Role.PROPERTY);
        return pointService.setEnclosure(id, req.until(), req.reason());
    }

    @Operation(summary = "解除临时围挡（物业）")
    @DeleteMapping("/{id}/enclosure")
    public WastePoint clearEnclosure(@PathVariable Long id) {
        CurrentUser.require(Role.PROPERTY);
        return pointService.clearEnclosure(id);
    }

    @Operation(summary = "查询点位实时占用袋数")
    @GetMapping("/{id}/occupied")
    public Map<String, Object> occupied(@PathVariable Long id) {
        return Map.of("pointId", id, "occupiedBags", capacityService.occupiedBags(id));
    }
}
