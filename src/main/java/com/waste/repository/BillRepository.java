package com.waste.repository;

import com.waste.entity.Bill;
import com.waste.enums.BillStatus;
import com.waste.enums.BillType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByBillTypeOrderByIdDesc(BillType type);
    List<Bill> findByStatusOrderByIdDesc(BillStatus status);
    List<Bill> findByPeriodOrderByIdDesc(String period);
    List<Bill> findByBillTypeAndPeriodOrderByIdDesc(BillType type, String period);
    List<Bill> findByAppointmentId(Long appointmentId);
    List<Bill> findByRegistrationIdOrderByIdDesc(Long registrationId);
}
