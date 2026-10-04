package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.AnomalyStatus;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.HaulTrip;
import com.community.waste.entity.WeighAnomaly;
import com.community.waste.repository.WeighAnomalyRepository;
import com.community.waste.service.WeighService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "6-进出场与称重", description = "车辆进场（空车重/车牌/司机/门禁）、出场（重车/袋数/照片/联单），净重分摊到袋、异常检测")
@RestController
@RequestMapping("/api/trips")
public class WeighController {

    public record EnterRequest(BigDecimal tareWeight, String gateRecord, String photos, LocalDateTime enterTime) {}
    public record ExitRequest(BigDecimal grossWeight, Integer loadedBagCount,
                              List<String> scannedBagCodes, String photos, LocalDateTime exitTime) {}
    public record ResolveAnomalyRequest(AnomalyStatus decision, BigDecimal resolvedWeight,
                                        String resolution, Long operatorId) {}

    private final WeighService weighService;
    private final WeighAnomalyRepository anomalyRepository;

    public WeighController(WeighService weighService, WeighAnomalyRepository anomalyRepository) {
        this.weighService = weighService;
        this.anomalyRepository = anomalyRepository;
    }

    @Operation(summary = "车辆进场登记：车牌、司机、空车重量、门禁记录与照片")
    @PostMapping("/{id}/enter")
    public HaulTrip enter(@PathVariable Long id, @RequestBody EnterRequest req) {
        CurrentUser.requireAny(Role.COMPANY, Role.PROPERTY);
        return weighService.vehicleEnter(id, req.tareWeight(), req.gateRecord(), req.photos(), req.enterTime());
    }

    @Operation(summary = "出场称重：重车重量、现场清点袋数、扫码核验袋编码、照片，自动算净重并按比例分摊到袋")
    @PostMapping("/{id}/exit")
    public HaulTrip exit(@PathVariable Long id, @RequestBody ExitRequest req) {
        CurrentUser.requireAny(Role.COMPANY, Role.PROPERTY);
        return weighService.vehicleExit(id, req.grossWeight(), req.loadedBagCount(),
                req.scannedBagCodes(), req.photos(), req.exitTime());
    }

    @Operation(summary = "称重异常列表（可按状态筛选）")
    @GetMapping("/anomalies")
    public List<WeighAnomaly> anomalies(@RequestParam(required = false) AnomalyStatus status) {
        return status == null ? anomalyRepository.findAll() : anomalyRepository.findByStatus(status);
    }

    @Operation(summary = "人工处理称重异常：确认正常/调整结算重量/忽略")
    @PostMapping("/anomalies/{id}/resolve")
    public WeighAnomaly resolve(@PathVariable Long id, @RequestBody ResolveAnomalyRequest req) {
        CurrentUser.requireAny(Role.PROPERTY, Role.FINANCE);
        return weighService.resolveAnomaly(id, req.decision(), req.resolvedWeight(),
                req.resolution(), req.operatorId());
    }
}
