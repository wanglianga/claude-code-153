package com.waste.repository;

import com.waste.entity.DispatchOrder;
import com.waste.enums.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DispatchOrderRepository extends JpaRepository<DispatchOrder, Long> {
    Optional<DispatchOrder> findByAppointmentId(Long appointmentId);
    List<DispatchOrder> findByVehicleIdAndPlannedArrivalBetween(Long vehicleId, LocalDateTime from, LocalDateTime to);
    List<DispatchOrder> findByStatusOrderByIdDesc(DispatchStatus status);
    List<DispatchOrder> findByCompanyNameOrderByIdDesc(String companyName);
}
