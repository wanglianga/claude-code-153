package com.community.waste.service;

import com.community.waste.common.BusinessException;
import com.community.waste.common.NoGenerator;
import com.community.waste.domain.DomainEnums.*;
import com.community.waste.entity.*;
import com.community.waste.mq.DomainEvent;
import com.community.waste.mq.EventPublisher;
import com.community.waste.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 处置场回执登记：复称、消纳费、差异异常检测、袋生命周期闭环。
 */
@Service
public class DisposalService {

    private final DisposalReceiptRepository receiptRepository;
    private final HaulTripRepository tripRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBagRepository appointmentBagRepository;
    private final WasteBagRepository bagRepository;
    private final WeighAnomalyRepository anomalyRepository;
    private final BillingService billingService;
    private final FeeService feeService;
    private final TraceService traceService;
    private final EventPublisher publisher;

    public DisposalService(DisposalReceiptRepository receiptRepository,
                           HaulTripRepository tripRepository,
                           AppointmentRepository appointmentRepository,
                           AppointmentBagRepository appointmentBagRepository,
                           WasteBagRepository bagRepository,
                           WeighAnomalyRepository anomalyRepository,
                           BillingService billingService,
                           FeeService feeService,
                           TraceService traceService,
                           EventPublisher publisher) {
        this.receiptRepository = receiptRepository;
        this.tripRepository = tripRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentBagRepository = appointmentBagRepository;
        this.bagRepository = bagRepository;
        this.anomalyRepository = anomalyRepository;
        this.billingService = billingService;
        this.feeService = feeService;
        this.traceService = traceService;
        this.publisher = publisher;
    }

    @Transactional
    public DisposalReceipt receive(Long tripId, String siteName, BigDecimal receivedWeight,
                                   BigDecimal unitPrice, LocalDateTime receivedAt, String operator,
                                   String photoUrl, String receiptNo) {
        HaulTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("车次不存在: " + tripId));
        if (trip.getStatus() != TripStatus.EXITED && trip.getStatus() != TripStatus.RECEIVED) {
            throw new BusinessException("车辆尚未完成出场称重，不能登记处置回执，当前 " + trip.getStatus());
        }
        if (trip.getNetWeight() == null || trip.getNetWeight().signum() <= 0) {
            throw new BusinessException("车次净重无效，请先处理称重异常再登记回执");
        }
        receiptRepository.findByTripId(tripId).ifPresent(r -> {
            throw new BusinessException("车次已有处置回执：" + r.getReceiptNo());
        });
        if (receivedWeight == null || receivedWeight.signum() <= 0) {
            throw new BusinessException("处置场复称重量必须大于0");
        }
        BigDecimal price = unitPrice == null ? BigDecimal.valueOf(0.15) : unitPrice;
        BigDecimal fee = receivedWeight.multiply(price).setScale(2, RoundingMode.HALF_UP);

        DisposalReceipt receipt = new DisposalReceipt();
        receipt.setTripId(tripId);
        receipt.setReceiptNo(receiptNo == null ? NoGenerator.next("RCPT") : receiptNo);
        receipt.setSiteName(siteName);
        receipt.setReceivedWeight(receivedWeight.setScale(2, RoundingMode.HALF_UP));
        receipt.setUnitPrice(price);
        receipt.setDisposalFee(fee);
        receipt.setReceivedAt(receivedAt == null ? LocalDateTime.now() : receivedAt);
        receipt.setOperator(operator);
        receipt.setPhotoUrl(photoUrl);
        receipt = receiptRepository.save(receipt);

        // 复称差异检测
        double diffLimit = feeService.get(FeeService.RECEIPT_DIFF_LIMIT, BigDecimal.valueOf(0.1)).doubleValue();
        BigDecimal diff = receivedWeight.subtract(trip.getNetWeight()).abs()
                .divide(trip.getNetWeight(), 4, RoundingMode.HALF_UP);
        if (diff.doubleValue() > diffLimit) {
            WeighAnomaly anomaly = new WeighAnomaly();
            anomaly.setAnomalyNo(NoGenerator.next("ANM"));
            anomaly.setTripId(tripId);
            anomaly.setReceiptId(receipt.getId());
            anomaly.setType("RECEIPT_DIFF");
            anomaly.setDetail(String.format("处置场复称与出场净重差异 %.1f%%（超 %.0f%%）：出场 %.2fkg，处置场 %.2fkg",
                    diff.doubleValue() * 100, diffLimit * 100, trip.getNetWeight(), receivedWeight));
            anomaly.setExpectedValue(trip.getNetWeight());
            anomaly.setActualValue(receivedWeight);
            anomaly.setStatus(AnomalyStatus.OPEN);
            anomalyRepository.save(anomaly);
            publisher.publish("waste.weigh.anomaly",
                    DomainEvent.of("WEIGH_ANOMALY", anomaly.getDetail()).ref(null, null, tripId)
                            .put("type", "RECEIPT_DIFF").put("anomalyId", anomaly.getId()));
        }

        // 袋生命周期闭环 DISPOSED，预约 COMPLETED
        List<Long> apptIds = trip.appointmentIdList();
        for (Long apptId : apptIds) {
            Appointment a = appointmentRepository.findById(apptId).orElse(null);
            if (a == null) continue;
            for (AppointmentBag ab : appointmentBagRepository.findByAppointmentId(apptId)) {
                WasteBag bag = bagRepository.findById(ab.getBagId()).orElse(null);
                if (bag != null) {
                    bag.setStatus(BagStatus.DISPOSED);
                    bag.setDisposedAt(receipt.getReceivedAt());
                    bagRepository.save(bag);
                    traceService.record(TraceType.DISPOSAL_RECEIPT, Role.COMPANY.name(),
                            "处置场 " + siteName + " 回执 " + receipt.getReceiptNo()
                                    + "：袋 " + bag.getBagCode() + " 已消纳（复称 "
                                    + receivedWeight + "kg，处置费 " + fee + " 元）",
                            bag.getRegistration().getId(), bag.getBagCode(), apptId, tripId,
                            null, null, null);
                }
            }
            a.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(a);
        }
        trip.setStatus(TripStatus.RECEIVED);
        tripRepository.save(trip);

        // 物业垫付处置费台账
        billingService.generateAdvanceForReceipt(receipt, trip);

        traceService.record(TraceType.DISPOSAL_RECEIPT, Role.COMPANY.name(),
                "车次 " + trip.getTripNo() + " 获处置场回执，消纳费 " + fee + " 元（物业垫付）",
                null, null, null, tripId, null, null, null);
        publisher.publish("waste.appointment.completed",
                DomainEvent.of("DISPOSAL_RECEIVED", "处置完成 " + trip.getTripNo())
                        .ref(null, null, tripId).put("receiptNo", receipt.getReceiptNo()));
        return receipt;
    }
}
