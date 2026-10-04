package com.community.waste.repository;

import com.community.waste.entity.Bill;
import com.community.waste.domain.DomainEnums.BillPartyType;
import com.community.waste.domain.DomainEnums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByRegistrationId(Long registrationId);
    List<Bill> findByPartyTypeAndPartyRef(BillPartyType partyType, String partyRef);
    List<Bill> findByStatus(BillStatus status);
    List<Bill> findByTripId(Long tripId);
}
