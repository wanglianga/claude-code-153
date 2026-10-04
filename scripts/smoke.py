#!/usr/bin/env python3
"""端到端冒烟脚本：对运行中的服务走完整业务链。用法: python3 smoke.py [baseUrl]"""
import json
import sys
import time
import urllib.request
import urllib.error

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
RUN = sys.argv[2] if len(sys.argv) > 2 else str(int(time.time()))[-6:]
TOKENS = {
    "property": "property-token",
    "company": "company-token",
    "finance": "finance-token",
    "crew": "crew-token",
    "owner": "owner-token",
}

def call(method, path, role=None, body=None, expect=200):
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if data:
        req.add_header("Content-Type", "application/json")
    if role:
        req.add_header("X-Auth-Token", TOKENS[role])
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode("utf-8")
            code = resp.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        code = e.code
    ok = code == expect
    parsed = None
    if raw:
        try:
            parsed = json.loads(raw)
        except Exception:
            pass
    print(f"[{ 'OK' if ok else 'FAIL'}] {method} {path} -> {code} (expect {expect})")
    if not ok:
        print("    ", raw[:300])
        raise SystemExit(1)
    return parsed

print("== 1. health / swagger ==")
call("GET", "/api/public/health")
docs = call("GET", "/v3/api-docs")
assert len(docs["paths"]) >= 50, docs["paths"]
print("    OpenAPI paths =", len(docs["paths"]))

print("== 2. 容量试算 P-B 空点 +15 => 75% 错峰 ==")
chk = call("POST", "/api/points/capacity-check", "crew", {"pointId": 2, "newBags": 15})
assert chk["decision"] == "PEAK_SHIFT", chk
print("    ", chk["message"])

print("== 3. 登记 + 物业审核 ==")
reg = call("POST", "/api/registrations", "property", {
    "communityCode": "DEFAULT", "communityName": "幸福里小区",
    "buildingNo": "9号楼", "roomNo": "R" + RUN, "ownerName": "陈业主",
    "crewName": "顺发施工队", "startDate": "2026-10-01",
    "expectedEndDate": "2026-11-01", "wasteTypes": "wood,glass",
    "haulFrequency": "每日1次", "deposit": 3000})
rid = reg["id"]
call("POST", f"/api/registrations/{rid}/approve", "property", {})

print("== 4. 接近上限未确认错峰 => 400 ==")
call("POST", "/api/reports", "crew", {
    "registrationId": rid, "reportDate": "2026-10-04", "bagCount": 15,
    "pileLocation": "B区集中垃圾点", "pointId": 2, "wood": True}, expect=400)

print("== 5. 确认错峰后通过 ==")
rep = call("POST", "/api/reports", "crew", {
    "registrationId": rid, "reportDate": "2026-10-04", "bagCount": 15,
    "pileLocation": "B区集中垃圾点", "pointId": 2, "wood": True,
    "acknowledgePeakShift": True})
assert len(rep["bags"]) == 15
print("    notice:", rep["notice"])
bag_code = rep["bags"][0]["bagCode"]

print("== 6. 物业审核日报生成预约 ==")
appt = call("POST", f"/api/reports/{rep['report']['id']}/audit", "property", {
    "approved": True, "scheduledTime": "2026-10-07T10:00:00",
    "deadline": "2026-10-07T12:00:00"})
aid = appt["id"]

print("== 7. 夜间派单被门禁时间窗拦截并给出建议时间 ==")
pre = call("POST", "/api/appointments/dispatch/precheck", "company", {
    "appointmentIds": [aid], "plate": "京A12345",
    "plannedArrival": "2026-10-07T22:00:00"})
assert pre["errors"], pre
assert any("作业时间窗" in e for e in pre["errors"])
print("    ", pre["errors"][0], "|", pre["warnings"][0])

print("== 8. 合规时间派单 ==")
trip = call("POST", "/api/appointments/dispatch", "company", {
    "appointmentIds": [aid], "plate": "京A12345", "driverId": 1,
    "plannedArrival": "2026-10-07T10:00:00", "routePlan": "P-B->处置场"})
tid = trip["id"]
call("POST", f"/api/company/trips/{tid}/enroute", "company", {})
call("POST", f"/api/trips/{tid}/enter", "company", {
    "tareWeight": 2000, "gateRecord": "南门10:02放行", "photos": "e.jpg"})

print("== 9. 出场称重 净重360kg（偏差 -4% 不告警） ==")
exited = call("POST", f"/api/trips/{tid}/exit", "company", {
    "grossWeight": 2360, "loadedBagCount": 15, "photos": "x.jpg"})
assert exited["netWeight"] == 360.0, exited
bag = call("GET", f"/api/trace/bag/{bag_code}", "owner")
assert bag["bag"]["actualKg"] == 24.0, bag  # 360/15

print("== 10. 处置场回执闭环 + 物业垫付 ==")
rcpt = call("POST", f"/api/disposal/trips/{tid}/receipt", "company", {
    "siteName": "南郊资源化处置场", "receivedWeight": 358,
    "unitPrice": 0.15, "operator": "刘工"})
assert rcpt["disposalFee"] == 53.7, rcpt
assert call("GET", f"/api/trace/bag/{bag_code}", "owner")["bag"]["status"] == "DISPOSED"

print("== 11. 四方账单 ==")
# 业主: 360*0.8 + 15*15 = 288 + 225 = 513
owner_bills = call("GET", f"/api/bills?registrationId={rid}", "finance")
fee_bill = [b for b in owner_bills if b["billType"] == "DISPOSAL_FEE"][0]
assert fee_bill["amount"] == 513.0, fee_bill
company_bills = call("GET", "/api/bills?partyType=COMPANY", "finance")
assert company_bills[0]["amount"] == 126.0  # 360*0.35
adv = call("GET", "/api/bills?partyType=PROPERTY_ADVANCE", "finance")
assert adv[0]["amount"] == 53.7
print("    业主清运费 513.0 / 公司结算 126.0 / 物业垫付 53.7")

print("== 12. 消防通道违规扣押金 ==")
vio = call("POST", "/api/violations", "property", {
    "type": "FIRE_LANE", "source": "INSPECTION",
    "description": "9号楼0301施工队垃圾堆占消防通道",
    "registrationId": rid, "liableParty": "CREW", "fineAmount": 500,
    "autoConfirm": True})
assert vio["status"] == "CONFIRMED"
bal = call("GET", f"/api/deposits/balance/{rid}", "property")
assert bal["balance"] == 2500.0, bal

print("== 13. 邻居投诉 -> 认定违规 -> 追责链 ==")
cmp = call("POST", "/api/complaints", "owner", {
    "complainantName": "周邻居", "complainantLocation": "9号楼3层",
    "registrationId": rid, "type": "DUST",
    "content": "0301装修垃圾敞开堆放扬尘"})
cid = cmp["id"]
call("POST", f"/api/complaints/{cid}/investigate", "property", {})
call("POST", f"/api/complaints/{cid}/resolve", "property", {
    "result": "要求加盖防尘网并限时清理", "liableParty": "CREW",
    "openViolation": True, "violationType": "ILLEGAL_LOCATION",
    "fineAmount": 200, "confirmNow": True})
chain = call("GET", f"/api/trace/complaint/{cid}", "owner")
assert len(chain["chain"]["bags"]) == 15
assert call("GET", f"/api/deposits/balance/{rid}", "property")["balance"] == 2300.0

print("== 14. 费用争议裁决 -30 ==")
did = call("POST", "/api/disputes", "owner", {
    "billId": fee_bill["id"], "raisedByRole": "OWNER",
    "raisedByName": "陈业主", "reason": "申请复核称重"} )["id"]
call("POST", f"/api/disputes/{did}/review", "finance", {
    "approveAdjustment": True, "adjustment": -30, "result": "复核减免30元"})
bill = call("GET", f"/api/bills/{fee_bill['id']}", "finance")
assert bill["payableAmount"] == 483.0, bill
call("POST", f"/api/bills/{fee_bill['id']}/pay", "finance", {"channel": "WECHAT"})

print("== 15. 看板 ==")
prop = call("GET", "/api/dashboards/property", "property")
comp = call("GET", "/api/dashboards/company", "company")
fin = call("GET", "/api/dashboards/finance", "finance")
perf = call("GET", "/api/dashboards/company/performance", "property")
assert perf["totalTrips"] >= 1
print("    物业活跃预约:", prop["summary"]["activeAppointments"],
      "| 公司车辆:", len(comp["vehicles"]),
      "| 财务四方:", list(fin.keys()))

print("\n全部冒烟断言通过 ✔")
