package com.community.waste.web;

import com.community.waste.common.CurrentUser;
import com.community.waste.domain.DomainEnums.Role;
import com.community.waste.entity.Driver;
import com.community.waste.entity.HaulTrip;
import com.community.waste.entity.Vehicle;
import com.community.waste.repository.DriverRepository;
import com.community.waste.repository.HaulTripRepository;
import com.community.waste.repository.VehicleRepository;
import com.community.waste.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "5-车辆/司机与车次", description = "清运公司车辆司机档案、车次状态、出发到场")
@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final HaulTripRepository tripRepository;
    private final DispatchService dispatchService;

    public CompanyController(VehicleRepository vehicleRepository, DriverRepository driverRepository,
                             HaulTripRepository tripRepository, DispatchService dispatchService) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "新增车辆（清运公司）")
    @PostMapping("/vehicles")
    public Vehicle createVehicle(@RequestBody Vehicle v) {
        CurrentUser.requireAny(Role.COMPANY, Role.PROPERTY);
        return vehicleRepository.save(v);
    }

    @Operation(summary = "车辆列表")
    @GetMapping("/vehicles")
    public List<Vehicle> vehicles() {
        return vehicleRepository.findAll();
    }

    @Operation(summary = "新增司机（清运公司）")
    @PostMapping("/drivers")
    public Driver createDriver(@RequestBody Driver d) {
        CurrentUser.requireAny(Role.COMPANY, Role.PROPERTY);
        return driverRepository.save(d);
    }

    @Operation(summary = "司机列表")
    @GetMapping("/drivers")
    public List<Driver> drivers() {
        return driverRepository.findAll();
    }

    @Operation(summary = "车次列表")
    @GetMapping("/trips")
    public List<HaulTrip> trips() {
        return tripRepository.findAll();
    }

    @Operation(summary = "车次详情（车牌/司机/空车重/装载重/联单/照片）")
    @GetMapping("/trips/{id}")
    public HaulTrip trip(@PathVariable Long id) {
        return dispatchService.mustGet(id);
    }

    @Operation(summary = "车辆出发（清运公司）")
    @PostMapping("/trips/{id}/enroute")
    public HaulTrip enroute(@PathVariable Long id) {
        CurrentUser.require(Role.COMPANY);
        return dispatchService.enroute(id);
    }
}
