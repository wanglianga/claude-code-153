package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.*;
import com.community.waste.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务结算：
 * - 车次称重完成：按业主（装修登记）生成清运费账单（按重 + 按袋 + 临时加袋附加），
 *   同时生成清运公司应付结算单（按净重结算）；
 * - 处置场回执：记录物业垫付处置费台账；
 * - 支持收款核销、争议调整。
 */
@Service
public class BillingService {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBagRepository appointmentBagRepository;
    private final WasteBagRepository bagRepository;
    private final RegistrationRepository registrationRepository;
    private final FeeService feeService;
    private final TraceService traceService;

    public BillingService(BillRepository billRepository,
                          AppointmentRepository appointmentRepository,
                          AppointmentBagRepository appointmentBagRepository,
                          WasteBagRepository bagRepository,
                          RegistrationRepository registrationRepository,
                          FeeService feeService,
                          TraceService traceService) {
        this.billRepository = billRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentBagRepository = appointmentBagRepository;
        this.bagRepository = bagRepository;
        this.registrationRepository = registrationRepository;
        this.feeService = feeService;
        this.traceService = traceService;
    }

    /** 称重完成后生成业主清运费账单与清运公司结算单（按车次，按业主聚合） */
    @Transactional
    public void generateTripBilling(HaulTrip trip, List<Appointment> appts) {
        if (trip.getNetWeight() == null || trip.getNetWeight().signum() <= 0) {
            return; // 净重异常挂起，待裁决后补账
        }
        // 按业主（装修登记）聚合
        Map<Long, BigDecimal> netByReg = new LinkedHashMap<>();
        Map<Long, Integer> bagsByReg = new LinkedHashMap<>();
        Map<Long, Integer> extraByReg = new LinkedHashMap<>();
        for (Appointment a : appts) {
            BigDecimal regNet = BigDecimal.ZERO;
            int bags = 0;
            int extra = 0;
            for (AppointmentBag ab : appointmentBagRepository.findByAppointmentId(a.getId())) {
                WasteBag bag = bagRepository.findById(ab.getBagId()).orElse(null);
                if (bag == null) continue;
                bags++;
                if (Boolean.TRUE.equals(bag.getExtra())) extra++;
                if (bag.getActualKg() != null) regNet = regNet.add(bag.getActualKg());
            }
            netByReg.merge(a.getRegistrationId(), regNet, BigDecimal::add);
            bagsByReg.merge(a.getRegistrationId(), bags, Integer::sum);
            extraByReg.merge(a.getRegistrationId(), extra, Integer::sum);
        }

        for (Map.Entry<Long, BigDecimal> e : netByReg.entrySet()) {
            Long regId = e.getKey();
            Registration reg = registrationRepository.findById(regId).orElse(null);
            String community = reg == null ? "DEFAULT" : reg.getCommunityCode();
            BigDecimal net = e.getValue().setScale(2, java.math.RoundingMode.HALF_UP);
            int bags = bagsByReg.get(regId);
            int extra = extraByReg.getOrDefault(regId, 0);

            BigDecimal pricePerKg = feeService.get(community,
                    FeeService.DISPOSAL_PRICE_PER_KG, BigDecimal.valueOf(0.8));
            BigDecimal bagFee = feeService.get(community,
                    FeeService.BAG_FEE, BigDecimal.valueOf(15));
            BigDecimal extraSurcharge = feeService.get(community,
                    FeeService.EXTRA_BAG_SURCHARGE, BigDecimal.valueOf(10));

            BigDecimal weightFee = net.multiply(pricePerKg).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal bagPart = bagFee.multiply(BigDecimal.valueOf(bags));
            BigDecimal extraPart = extraSurcharge.multiply(BigDecimal.valueOf(extra));
            BigDecimal total = weightFee.add(bagPart).add(extraPart);

            Bill bill = new Bill();
            bill.setBillNo(NoGenerator.next("BIL"));
            bill.setBillType(BillType.DISPOSAL_FEE);
            bill.setPartyType(BillPartyType.OWNER);
            bill.setPartyRef(String.valueOf(regId));
            bill.setRegistrationId(regId);
            bill.setTripId(trip.getId());
            bill.setWeightKg(net);
            bill.setQuantity(bags);
            bill.setUnitPrice(pricePerKg);
            bill.setAmount(total);
            bill.setReceivable(true);
            bill.setStatus(BillStatus.UNPAID);
            bill.setSummary("车次 " + trip.getTripNo() + " 清运费：净重 " + net + "kg×" + pricePerKg
                    + " 元/kg=" + weightFee + "；装袋 " + bags + " 个×" + bagFee + " 元=" + bagPart
                    + (extra > 0 ? "；临时加袋 " + extra + " 个附加 " + extraPart : ""));
            bill = billRepository.save(bill);

            traceService.record(TraceType.BILL_GENERATED, Role.FINANCE.name(),
                    "生成业主清运费账单 " + bill.getBillNo() + "，合计 " + total + " 元（" + bill.getSummary() + "）",
                    regId, null, null, trip.getId(), bill.getId(), null, null);
        }

        // 清运公司应付结算单（按净重）
        String companyName = "清运公司";
        BigDecimal settlePrice = feeService.get(FeeService.COMPANY_SETTLE_PER_KG, BigDecimal.valueOf(0.35));
        BigDecimal companyAmount = trip.getNetWeight().multiply(settlePrice)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        Bill companyBill = new Bill();
        companyBill.setBillNo(NoGenerator.next("BIL"));
        companyBill.setBillType(BillType.COMPANY_SETTLEMENT);
        companyBill.setPartyType(BillPartyType.COMPANY);
        companyBill.setPartyRef(trip.getPlate());
        companyBill.setTripId(trip.getId());
        companyBill.setWeightKg(trip.getNetWeight());
        companyBill.setUnitPrice(settlePrice);
        companyBill.setAmount(companyAmount);
        companyBill.setReceivable(false); // 应付款
        companyBill.setStatus(BillStatus.UNPAID);
        companyBill.setSummary("车次 " + trip.getTripNo() + "（" + trip.getPlate() + "）清运服务费："
                + trip.getNetWeight() + "kg×" + settlePrice + " 元/kg");
        companyBill = billRepository.save(companyBill);
        traceService.record(TraceType.BILL_GENERATED, Role.FINANCE.name(),
                "生成清运公司结算单 " + companyBill.getBillNo() + "，应付 " + companyAmount + " 元",
                null, null, null, trip.getId(), companyBill.getId(), null, null);
    }

    /** 处置场回执：物业垫付处置费台账 */
    @Transactional
    public Bill generateAdvanceForReceipt(DisposalReceipt receipt, HaulTrip trip) {
        Bill bill = new Bill();
        bill.setBillNo(NoGenerator.next("BIL"));
        bill.setBillType(BillType.ADVANCE_STATEMENT);
        bill.setPartyType(BillPartyType.PROPERTY_ADVANCE);
        bill.setPartyRef("COMMUNITY");
        bill.setTripId(trip.getId());
        bill.setReceiptId(receipt.getId());
        bill.setWeightKg(receipt.getReceivedWeight());
        bill.setUnitPrice(receipt.getUnitPrice());
        bill.setAmount(receipt.getDisposalFee());
        bill.setReceivable(false); // 物业向处置场支付（垫付）
        bill.setStatus(BillStatus.UNPAID);
        bill.setSummary("物业垫付处置场 " + receipt.getSiteName() + " 消纳费，回执 "
                + receipt.getReceiptNo() + "，车次 " + trip.getTripNo());
        bill = billRepository.save(bill);
        traceService.record(TraceType.BILL_GENERATED, Role.FINANCE.name(),
                "登记物业垫付台账 " + bill.getBillNo() + "：" + receipt.getDisposalFee() + " 元",
                null, null, null, trip.getId(), bill.getId(), null, null);
        return bill;
    }

    @Transactional
    public Bill markPaid(Long billId, String channel) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("账单不存在: " + billId));
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("账单已支付");
        }
        if (bill.getStatus() == BillStatus.DISPUTED) {
            throw new BusinessException("账单争议处理中，不能核销");
        }
        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(LocalDateTime.now());
        bill.setPaidChannel(channel == null ? "OFFLINE" : channel);
        return billRepository.save(bill);
    }
}
