package com.community.waste;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全流程端到端：
 * 登记→审核→日报（容量错峰）→审核生成预约→派单（限行校验）→进场→称重分摊→处置回执→
 * 四方账单→违规押金扣款→投诉追责→费用争议→单袋追溯。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper json = new ObjectMapper();

    private static final HttpHeaders PROPERTY = headers("property-token");
    private static final HttpHeaders COMPANY = headers("company-token");
    private static final HttpHeaders FINANCE = headers("finance-token");
    private static final HttpHeaders CREW = headers("crew-token");
    private static final HttpHeaders OWNER = headers("owner-token");

    private static HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Auth-Token", token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private static Long regId;
    private static Long regId2;
    private static Long appointmentId;
    private static Long appointmentId2;
    private static Long tripId;
    private static Long ownerBillId;
    private static String bagCode;

    private JsonNode post(String path, Object body, HttpHeaders headers) {
        ResponseEntity<String> resp = rest.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful())
                .as("POST %s -> %s: %s", path, resp.getStatusCode(), resp.getBody()).isTrue();
        return parse(resp.getBody());
    }

    private JsonNode parse(String body) {
        try {
            return json.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode get(String path, HttpHeaders headers) {
        ResponseEntity<String> resp = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).as("GET %s: %s", path, resp.getBody()).isTrue();
        return parse(resp.getBody());
    }

    @Test
    @Order(1)
    void rejectsUnauthenticated() {
        ResponseEntity<String> resp = rest.getForEntity("/api/points", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(2)
    void registersAndApproves() {
        JsonNode r = post("/api/registrations", """
                {
                  "communityCode":"DEFAULT","communityName":"幸福里小区",
                  "buildingNo":"3号楼","roomNo":"1202","ownerName":"王业主","ownerPhone":"13900001111",
                  "crewName":"鑫达施工队","crewPhone":"13700002222",
                  "startDate":"2026-10-01","expectedEndDate":"2026-11-01",
                  "wasteTypes":"wood,ceramic","haulFrequency":"每日1次","deposit":1000
                }""", PROPERTY);
        regId = r.get("id").asLong();
        post("/api/registrations/" + regId + "/approve", "{}", PROPERTY);
        JsonNode detail = get("/api/registrations/" + regId, PROPERTY);
        assertThat(detail.get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @Order(3)
    void crewDailyReportPassesCapacityAndAuditCreatesAppointment() {
        JsonNode r = post("/api/reports", """
                {
                  "registrationId":%d,"reportDate":"2026-10-07","bagCount":4,
                  "pileLocation":"A区集中垃圾点西侧划线区","pointId":1,
                  "wood":true,"ceramic":true
                }""".formatted(regId), CREW);
        assertThat(r.get("bags")).hasSize(4);
        bagCode = r.get("bags").get(0).get("bagCode").asText();

        JsonNode appt = post("/api/reports/1/audit", """
                {
                  "approved":true,"remark":"袋装完好，分类属实",
                  "scheduledTime":"2026-10-07T10:00:00","deadline":"2026-10-07T20:00:00"
                }""", PROPERTY);
        appointmentId = appt.get("id").asLong();
        assertThat(appt.get("apptNo").asText()).startsWith("APT");
    }

    @Test
    @Order(4)
    void dispatchRespectsGateAndRestrictionThenDispatches() {
        // 夜间 22:00 不在进场时间窗 → 校验失败
        JsonNode bad = post("/api/appointments/dispatch/precheck", """
                {"appointmentIds":[%d],"plate":"京A12345","plannedArrival":"2026-10-07T22:00:00"}"""
                .formatted(appointmentId), COMPANY);
        assertThat(bad.get("errors").get(0).asText()).contains("作业时间窗");
        assertThat(bad.get("warnings").toString()).contains("下一可进场时间");

        // 周三 10:00 窗口内 → 通过
        JsonNode ok = post("/api/appointments/dispatch/precheck", """
                {"appointmentIds":[%d],"plate":"京A12345","plannedArrival":"2026-10-07T10:00:00"}"""
                .formatted(appointmentId), COMPANY);
        assertThat(ok.get("errors")).isEmpty();

        JsonNode trip = post("/api/appointments/dispatch", """
                {"appointmentIds":[%d],"plate":"京A12345","driverId":1,
                 "plannedArrival":"2026-10-07T10:00:00","routePlan":"P-A -> 处置场"}"""
                .formatted(appointmentId), COMPANY);
        tripId = trip.get("id").asLong();
        assertThat(trip.get("manifestNo").asText()).startsWith("MF");
    }

    @Test
    @Order(5)
    void vehicleWeighInOutAndBilling() {
        post("/api/company/trips/" + tripId + "/enroute", "{}", COMPANY);
        JsonNode entered = post("/api/trips/" + tripId + "/enter", """
                {"tareWeight":2000,"gateRecord":"南门门禁09:52放行，车牌京A12345","photos":"enter-1.jpg"}""", COMPANY);
        assertThat(entered.get("status").asText()).isEqualTo("ENTERED");

        JsonNode exited = post("/api/trips/" + tripId + "/exit", """
                {"grossWeight":2120,"loadedBagCount":4,"photos":"exit-1.jpg,load-1.jpg"}""", COMPANY);
        assertThat(exited.get("netWeight").asDouble()).isEqualTo(120.0);

        // 每袋按预估重量比例分摊净重（4袋各 30kg），称重完成袋状态为 WEIGHED
        JsonNode bagTrace = get("/api/trace/bag/" + bagCode, OWNER);
        assertThat(bagTrace.get("bag").get("status").asText()).isEqualTo("WEIGHED");
        assertThat(bagTrace.get("bag").get("actualKg").asDouble()).isEqualTo(30.0);
    }

    @Test
    @Order(6)
    void disposalReceiptClosesLoopAndCreatesBills() {
        JsonNode receipt = post("/api/disposal/trips/" + tripId + "/receipt", """
                {"siteName":"南郊资源化处置场","receivedWeight":119,"unitPrice":0.15,
                 "operator":"场站刘工","photoUrl":"receipt-1.jpg"}""", COMPANY);
        assertThat(receipt.get("disposalFee").asDouble()).isEqualTo(17.85);

        // 回执后袋生命周期闭环 DISPOSED
        assertThat(get("/api/trace/bag/" + bagCode, OWNER).get("bag").get("status").asText())
                .isEqualTo("DISPOSED");

        JsonNode bills = get("/api/bills?registrationId=" + regId, FINANCE);
        // 业主清运费：120*0.8 + 4*15 = 156
        JsonNode disposalBill = null;
        for (JsonNode b : bills) {
            if (b.get("billType").asText().equals("DISPOSAL_FEE")) {
                disposalBill = b;
            }
        }
        assertThat(disposalBill).isNotNull();
        assertThat(disposalBill.get("amount").asDouble()).isEqualTo(156.0);
        ownerBillId = disposalBill.get("id").asLong();

        JsonNode all = get("/api/bills?partyType=COMPANY", FINANCE);
        assertThat(all.get(0).get("amount").asDouble()).isEqualTo(42.0); // 120 * 0.35

        JsonNode advances = get("/api/bills?partyType=PROPERTY_ADVANCE", FINANCE);
        assertThat(advances.get(0).get("amount").asDouble()).isEqualTo(17.85);
    }

    @Test
    @Order(7)
    void fireLaneViolationDeductsDeposit() {
        JsonNode v = post("/api/violations", """
                {"type":"FIRE_LANE","source":"INSPECTION",
                 "description":"巡检发现3号楼1202施工队将两袋垃圾堆在消防通道",
                 "registrationId":%d,"liableParty":"CREW","fineAmount":500,
                 "evidence":"firelane.jpg","autoConfirm":true}""".formatted(regId), PROPERTY);
        assertThat(v.get("status").asText()).isEqualTo("CONFIRMED");

        JsonNode balance = get("/api/deposits/balance/" + regId, PROPERTY);
        assertThat(balance.get("balance").asDouble()).isEqualTo(500.0);
    }

    @Test
    @Order(8)
    void complaintCanTraceAccountability() {
        JsonNode c = post("/api/complaints", """
                {"complainantName":"赵邻居","complainantPhone":"13600003333",
                 "complainantLocation":"3号楼12层楼道","registrationId":%d,
                 "type":"DUST","content":"1202装修垃圾堆放开盖，扬尘明显"}""".formatted(regId), OWNER);
        Long complaintId = c.get("id").asLong();
        post("/api/complaints/" + complaintId + "/investigate", "{}", PROPERTY);
        JsonNode resolved = post("/api/complaints/" + complaintId + "/resolve", """
                {"result":"已要求施工队加盖防尘网并限时清理","liableParty":"CREW",
                 "openViolation":true,"violationType":"ILLEGAL_LOCATION","fineAmount":200,"confirmNow":true}""",
                PROPERTY);
        assertThat(resolved.get("status").asText()).isEqualTo("RESOLVED");

        JsonNode chain = get("/api/trace/complaint/" + complaintId, OWNER);
        assertThat(chain.get("chain").get("bags").size()).isGreaterThan(0);
        assertThat(chain.get("chain").get("timeline").toString()).contains("装修登记");
        // 罚款 500 + 200 已扣，押金余额 300
        assertThat(get("/api/deposits/balance/" + regId, PROPERTY).get("balance").asDouble())
                .isEqualTo(300.0);
    }

    @Test
    @Order(9)
    void disputeAdjustsBill() {
        JsonNode d = post("/api/disputes", """
                {"billId":%d,"raisedByRole":"OWNER","raisedByName":"王业主",
                 "reason":"称重分摊有疑问，申请复核","evidence":"wxchat.jpg"}""".formatted(ownerBillId), OWNER);
        Long disputeId = d.get("id").asLong();
        post("/api/disputes/" + disputeId + "/review", """
                {"approveAdjustment":true,"adjustment":-20,"result":"复核照片后给予20元减免"}""", FINANCE);
        JsonNode bill = get("/api/bills/" + ownerBillId, FINANCE);
        assertThat(bill.get("payableAmount").asDouble()).isEqualTo(136.0);
        post("/api/bills/" + ownerBillId + "/pay", "{\"channel\":\"WECHAT\"}", FINANCE);
        assertThat(get("/api/bills/" + ownerBillId, FINANCE).get("status").asText()).isEqualTo("PAID");
    }

    @Test
    @Order(10)
    void nearFullPointRequiresPeakShiftAckAndEnclosureExpands() {
        JsonNode r = post("/api/registrations", """
                {
                  "communityCode":"DEFAULT","communityName":"幸福里小区",
                  "buildingNo":"6号楼","roomNo":"0801","ownerName":"李业主",
                  "crewName":"宏远施工队","startDate":"2026-10-01","expectedEndDate":"2026-11-10",
                  "wasteTypes":"mixed","haulFrequency":"隔日1次","deposit":2000
                }""", PROPERTY);
        regId2 = r.get("id").asLong();
        post("/api/registrations/" + regId2 + "/approve", "{}", PROPERTY);

        // P-B 容量20，阈值75%：15袋即 75% 触发错峰提示
        HttpStatusExpectation expect400 = () -> {
            ResponseEntity<String> resp = rest.exchange("/api/reports", HttpMethod.POST,
                    new HttpEntity<>("""
                            {"registrationId":%d,"reportDate":"2026-10-08","bagCount":15,
                             "pileLocation":"B区集中垃圾点","pointId":2,"wood":true}""".formatted(regId2), CREW),
                    String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).contains("错峰");
        };
        expect400.run();

        JsonNode accepted = post("/api/reports", """
                {"registrationId":%d,"reportDate":"2026-10-08","bagCount":15,
                 "pileLocation":"B区集中垃圾点","pointId":2,"wood":true,
                 "acknowledgePeakShift":true}""".formatted(regId2), CREW);
        assertThat(accepted.get("notice").asText()).contains("错峰");
    }

    @Test
    @Order(11)
    void weighDeviationOpensAnomaly() {
        JsonNode appt = post("/api/reports/2/audit", """
                {"approved":true,"scheduledTime":"2026-10-08T15:00:00",
                 "deadline":"2026-10-09T12:00:00"}""", PROPERTY);
        appointmentId2 = appt.get("id").asLong();

        JsonNode trip2 = post("/api/appointments/dispatch", """
                {"appointmentIds":[%d],"plate":"京A12345","driverId":1,
                 "plannedArrival":"2026-10-08T15:00:00","routePlan":"P-B -> 处置场"}"""
                .formatted(appointmentId2), COMPANY);
        Long t2 = trip2.get("id").asLong();
        post("/api/company/trips/" + t2 + "/enroute", "{}", COMPANY);
        post("/api/trips/" + t2 + "/enter", """
                {"tareWeight":2000,"gateRecord":"东门14:50放行"}""", COMPANY);
        // 预估 15*25=375kg，实称净重 200kg，偏差 46.7% > 35% → DEVIATION 异常
        post("/api/trips/" + t2 + "/exit",
                "{\"grossWeight\":2200,\"loadedBagCount\":15}", COMPANY);

        JsonNode anomalies = get("/api/trips/anomalies?status=OPEN", PROPERTY);
        assertThat(anomalies.toString()).contains("DEVIATION");

        // 异常可人工裁决调整
        Long anomalyId = anomalies.get(0).get("id").asLong();
        post("/api/trips/anomalies/" + anomalyId + "/resolve", """
                {"decision":"CONFIRMED_NORMAL","resolution":"核对装车照片，袋装偏轻属实，按实称结算"}""",
                PROPERTY);
    }

    @Test
    @Order(12)
    void dashboardsWork() {
        JsonNode property = get("/api/dashboards/property", PROPERTY);
        assertThat(property.get("summary").get("activeAppointments").asInt()).isGreaterThanOrEqualTo(0);
        JsonNode company = get("/api/dashboards/company", COMPANY);
        assertThat(company.get("vehicles").size()).isGreaterThanOrEqualTo(2);
        JsonNode finance = get("/api/dashboards/finance", FINANCE);
        assertThat(finance.has("OWNER")).isTrue();
        JsonNode perf = get("/api/dashboards/company/performance", PROPERTY);
        assertThat(perf.get("totalTrips").asInt()).isGreaterThanOrEqualTo(2);
    }

    @FunctionalInterface
    interface HttpStatusExpectation {
        void run();
    }
}
