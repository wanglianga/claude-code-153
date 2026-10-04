package com.community.waste.bootstrap;

import com.community.waste.entity.AccessRule;
import com.community.waste.entity.Driver;
import com.community.waste.entity.FeeConfig;
import com.community.waste.entity.Vehicle;
import com.community.waste.entity.WastePoint;
import com.community.waste.repository.*;
import com.community.waste.service.FeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 初始化演示基础数据（幂等）：计费参数、垃圾点、门禁限行规则、车辆与司机。 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final FeeConfigRepository feeConfigRepository;
    private final WastePointRepository pointRepository;
    private final AccessRuleRepository accessRuleRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public DataInitializer(FeeConfigRepository feeConfigRepository, WastePointRepository pointRepository,
                           AccessRuleRepository accessRuleRepository, VehicleRepository vehicleRepository,
                           DriverRepository driverRepository) {
        this.feeConfigRepository = feeConfigRepository;
        this.pointRepository = pointRepository;
        this.accessRuleRepository = accessRuleRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initFees();
        initPoints();
        initAccessRules();
        initVehicles();
        log.info("演示基础数据初始化完成");
    }

    private void initFees() {
        fee(FeeService.DISPOSAL_PRICE_PER_KG, "0.80", "业主清运费单价 元/kg");
        fee(FeeService.BAG_FEE, "15", "按袋服务费 元/袋");
        fee(FeeService.EXTRA_BAG_SURCHARGE, "10", "临时加袋附加费 元/袋");
        fee(FeeService.COMPANY_SETTLE_PER_KG, "0.35", "清运公司结算单价 元/kg");
        fee(FeeService.FINE_FIRE_LANE, "500", "堆占消防通道罚款");
        fee(FeeService.FINE_MIXED, "300", "混入生活垃圾罚款");
        fee(FeeService.FINE_ILLEGAL, "200", "违规堆放罚款");
        fee(FeeService.FINE_LATE, "200", "车辆迟到罚款");
        fee(FeeService.FINE_SPILL, "800", "迟到致垃圾点溢出罚款");
        fee(FeeService.ENCLOSURE_CAPACITY_FACTOR, "1.5", "临时围挡扩容系数");
        fee(FeeService.WEIGH_DEVIATION_LIMIT, "0.35", "称重与预估偏差告警阈值");
        fee(FeeService.RECEIPT_DIFF_LIMIT, "0.10", "处置场复称差异告警阈值");
        fee(FeeService.DEFAULT_BAG_KG, "25", "标准袋预估重量 kg");
    }

    private void fee(String key, String value, String remark) {
        feeConfigRepository.findByCommunityCodeAndParamKey("DEFAULT", key)
                .orElseGet(() -> feeConfigRepository.save(
                        new FeeConfig(key, new BigDecimal(value), remark)));
    }

    private void initPoints() {
        if (pointRepository.count() > 0) return;
        WastePoint p1 = new WastePoint();
        p1.setCommunityCode("DEFAULT");
        p1.setCode("P-A");
        p1.setName("A区集中垃圾点");
        p1.setLocation("1-3号楼地下车库入口西侧");
        p1.setCapacityBags(40);
        p1.setCapacityKg(new BigDecimal("1000"));
        p1.setNearFullThreshold(0.8);
        pointRepository.save(p1);

        WastePoint p2 = new WastePoint();
        p2.setCommunityCode("DEFAULT");
        p2.setCode("P-B");
        p2.setName("B区集中垃圾点");
        p2.setLocation("5-8号楼北侧围挡区");
        p2.setCapacityBags(20);
        p2.setCapacityKg(new BigDecimal("500"));
        p2.setNearFullThreshold(0.75);
        pointRepository.save(p2);
    }

    private void initAccessRules() {
        if (accessRuleRepository.count() > 0) return;
        // 每天允许 08:00-11:30 与 14:00-17:00 进场清运
        AccessRule r1 = new AccessRule();
        r1.setCommunityCode("DEFAULT");
        r1.setRuleName("白天作业时间窗(上午)");
        r1.setWeekday(0);
        r1.setAllowedFrom(LocalTime.of(8, 0));
        r1.setAllowedTo(LocalTime.of(11, 30));
        accessRuleRepository.save(r1);

        AccessRule r2 = new AccessRule();
        r2.setCommunityCode("DEFAULT");
        r2.setRuleName("白天作业时间窗(下午)");
        r2.setWeekday(0);
        r2.setAllowedFrom(LocalTime.of(14, 0));
        r2.setAllowedTo(LocalTime.of(17, 0));
        accessRuleRepository.save(r2);

        // 周一限行尾号 1、6；周五限行 4、9
        AccessRule r3 = new AccessRule();
        r3.setCommunityCode("DEFAULT");
        r3.setRuleName("周一尾号限行");
        r3.setWeekday(1);
        r3.setRestrictedTailDigits("1,6");
        accessRuleRepository.save(r3);

        AccessRule r4 = new AccessRule();
        r4.setCommunityCode("DEFAULT");
        r4.setRuleName("周五尾号限行");
        r4.setWeekday(5);
        r4.setRestrictedTailDigits("4,9");
        accessRuleRepository.save(r4);
    }

    private void initVehicles() {
        if (vehicleRepository.count() > 0) return;
        Vehicle v1 = new Vehicle();
        v1.setPlate("京A12345");
        v1.setTailDigit("5");
        v1.setCapacityKg(3000);
        v1.setCapacityBags(80);
        v1.setCompanyName("净城清运有限公司");
        vehicleRepository.save(v1);

        Vehicle v2 = new Vehicle();
        v2.setPlate("京B67891");
        v2.setTailDigit("1");
        v2.setCapacityKg(2000);
        v2.setCapacityBags(60);
        v2.setCompanyName("净城清运有限公司");
        vehicleRepository.save(v2);

        Driver d1 = new Driver();
        d1.setName("张师傅");
        d1.setPhone("13800000001");
        d1.setLicenseNo("DRV001");
        d1.setCompanyName("净城清运有限公司");
        driverRepository.save(d1);

        Driver d2 = new Driver();
        d2.setName("李师傅");
        d2.setPhone("13800000002");
        d2.setLicenseNo("DRV002");
        d2.setCompanyName("净城清运有限公司");
        driverRepository.save(d2);
    }
}
