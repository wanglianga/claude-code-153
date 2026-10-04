package com.community.waste.web;

import com.community.waste.domain.DomainEnums.AppointmentStatus;
import com.community.waste.entity.Appointment;
import com.community.waste.repository.AppointmentRepository;
import com.community.waste.service.DispatchService;
import com.community.waste.entity.HaulTrip;
import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "4-清运预约与派单", description = "物业查看预约；清运公司按门禁/限行/容量/已有派单预校验并排车派单")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository repository;
    private final DispatchService dispatchService;

    public AppointmentController(AppointmentRepository repository, DispatchService dispatchService) {
        this.repository = repository;
        this.dispatchService = dispatchService;
    }

    public record DispatchPreCheckRequest(List<Long> appointmentIds, String plate, LocalDateTime plannedArrival) {}
    public record DispatchRequest(List<Long> appointmentIds, String plate, Long driverId,
                                  LocalDateTime plannedArrival, String routePlan) {}

    @Operation(summary = "预约列表（可按状态筛选）")
    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) AppointmentStatus status) {
        if (status != null) {
            return repository.findByStatusIn(List.of(status));
        }
        return repository.findAll();
    }

    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public Appointment get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Operation(summary = "某房号（装修登记）的预约")
    @GetMapping("/by-registration/{registrationId}")
    public List<Appointment> byRegistration(@PathVariable Long registrationId) {
        return repository.findByRegistrationId(registrationId);
    }

    @Operation(summary = "派单预校验：校验车辆载重/袋容、门禁时间窗、尾号限行、已有派单冲突，返回错误与建议时间")
    @PostMapping("/dispatch/precheck")
    public DispatchService.DispatchPlanCheck precheck(@RequestBody DispatchPreCheckRequest req) {
        CurrentUser.require(Role.COMPANY);
        return dispatchService.preCheck(req.appointmentIds(), req.plate(), req.plannedArrival());
    }

    @Operation(summary = "清运公司正式派单（创建车次、联单，预约转已派单）")
    @PostMapping("/dispatch")
    public HaulTrip dispatch(@RequestBody DispatchRequest req) {
        CurrentUser.require(Role.COMPANY);
        return dispatchService.dispatch(req.appointmentIds(), req.plate(), req.driverId(),
                req.plannedArrival(), req.routePlan());
    }

    @Operation(summary = "按点位查看待清运预约")
    @GetMapping("/by-point/{pointId}")
    public Map<String, Object> byPoint(@PathVariable Long pointId) {
        List<Appointment> appts = repository.findByPointIdAndStatusIn(pointId,
                List.of(AppointmentStatus.APPROVED, AppointmentStatus.DISPATCHED, AppointmentStatus.ARRIVED));
        return Map.of("pointId", pointId, "appointments", appts, "count", appts.size());
    }
}
