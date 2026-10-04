package com.waste.config;

import com.waste.entity.RenovationRegistration;
import com.waste.entity.User;
import com.waste.entity.Vehicle;
import com.waste.entity.WastePoint;
import com.waste.enums.Frequency;
import com.waste.enums.Role;
import com.waste.repository.RenovationRegistrationRepository;
import com.waste.repository.UserRepository;
import com.waste.repository.VehicleRepository;
import com.waste.repository.WastePointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 首次启动初始化演示数据（幂等：已有用户则跳过） */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final WastePointRepository wastePointRepo;
    private final VehicleRepository vehicleRepo;
    private final RenovationRegistrationRepository regRepo;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            return;
        }
        log.info("初始化演示数据...");

        // ---- 测试账号（密码均为 123456，admin 为 admin123）----
        saveUser("admin", "admin123", "系统管理员", Role.ADMIN, null, null);
        saveUser("owner1", "123456", "张三", Role.OWNER, "13800001001", null);
        saveUser("owner2", "123456", "张四", Role.OWNER, "13800001002", null);
        saveUser("worker1", "123456", "李工", Role.WORKER, "13800002001", "宏远装修队");
        saveUser("property1", "123456", "王经理", Role.PROPERTY, "13800003001", null);
        saveUser("collector1", "123456", "赵调度", Role.COLLECTOR, "13800004001", "绿洁清运");
        saveUser("finance1", "123456", "陈会计", Role.FINANCE, "13800005001", null);

        // ---- 垃圾点（2 号楼点已接近上限，用于演示容量规则）----
        savePoint("1号楼装修垃圾点", "1号楼东侧空地", 100, 20);
        savePoint("2号楼装修垃圾点", "2号楼西侧空地", 50, 46);
        savePoint("3号楼临时堆放点", "3号楼北侧围挡区", 30, 5);

        // ---- 清运车辆（沪C11116 尾号 6，周一限行，用于演示限行规则）----
        saveVehicle("沪A12345", "5.0", "刘师傅", "13900000001", "绿洁清运");
        saveVehicle("沪B67890", "8.0", "陈师傅", "13900000002", "绿洁清运");
        saveVehicle("沪C11116", "3.0", "王师傅", "13900000003", "绿洁清运");

        // ---- 示例装修登记（业主张三，1栋801室，押金 2000）----
        User owner1 = userRepo.findByUsername("owner1").orElseThrow();
        RenovationRegistration reg = new RenovationRegistration();
        reg.setRegNo("REG-SEED-0001");
        reg.setOwnerId(owner1.getId());
        reg.setOwnerName(owner1.getName());
        reg.setBuilding("1栋");
        reg.setRoomNumber("801");
        reg.setConstructionTeam("宏远装修队");
        reg.setExpectedStartDate(LocalDate.now().minusDays(2));
        reg.setExpectedEndDate(LocalDate.now().plusDays(18));
        reg.setWasteTypes("木板,玻璃,瓷砖");
        reg.setFrequency(Frequency.DAILY);
        reg.setDepositAmount(new BigDecimal("2000.00"));
        reg.setDepositBalance(new BigDecimal("2000.00"));
        regRepo.save(reg);

        log.info("演示数据初始化完成：7 个账号 / 3 个垃圾点 / 3 辆车 / 1 个装修登记");
    }

    private void saveUser(String username, String password, String name, Role role, String phone, String team) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setName(name);
        u.setRole(role);
        u.setPhone(phone);
        u.setTeamName(team);
        userRepo.save(u);
    }

    private void savePoint(String name, String location, int capacity, int current) {
        WastePoint p = new WastePoint();
        p.setName(name);
        p.setLocation(location);
        p.setCapacityBags(capacity);
        p.setCurrentBags(current);
        wastePointRepo.save(p);
    }

    private void saveVehicle(String plate, String tons, String driver, String phone, String company) {
        Vehicle v = new Vehicle();
        v.setPlateNo(plate);
        v.setCapacityTons(new BigDecimal(tons));
        v.setDriverName(driver);
        v.setDriverPhone(phone);
        v.setCompanyName(company);
        vehicleRepo.save(v);
    }
}
