# 小区装修垃圾预约清运称重计费投诉追责服务

面向物业小区装修垃圾全流程管理的后端服务：业主装修登记 → 施工队每日堆放上报 → 物业审核生成清运预约 →
清运公司按门禁/限行/容量/已有派单排车 → 车辆进出场称重与清运联单 → 处置场回执 →
按业主/施工队/物业垫付/清运公司四方结算 → 违规、投诉、押金扣款、费用争议全程可追责。

技术栈：**Java 17 + Spring Boot 3.2 + Spring Data JPA + MySQL 8 + RabbitMQ 3.13 + springdoc-openapi**。

---

## 一、能力清单（对应业务场景）

| 业务场景 | 实现方式 |
|---|---|
| 装修前登记房号/施工队/工期/垃圾类型/清运频次/押金 | `POST /api/registrations` + 物业审核 |
| 施工队每日提交袋装数量、堆放位置、木板/玻璃/瓷砖分类 | `POST /api/reports`，自动生成带编码的垃圾袋（贴码可追溯） |
| 业主临时加袋 | `POST /api/reports/extra-bags`，加袋带附加费 |
| 物业审核后生成清运预约 | `POST /api/reports/{id}/audit`，袋状态 REPORTED→ACCEPTED→SCHEDULED |
| 门禁时间窗、车辆尾号限行、车辆载重/袋容、已有派单冲突 | `POST /api/appointments/dispatch/precheck`，违规拦截并建议下一可进场时间 |
| 车辆进出场：车牌、司机、空车重、装载重、照片、清运联单 | `/api/trips/{id}/enter`、`/exit`，自动生成联单号、净重按比例分摊到单袋 |
| 处置场回执 | `POST /api/disposal/trips/{tripId}/receipt`，复称差异检测、袋/预约闭环 |
| 垃圾堆到消防通道 | 日报位置含“消防通道”自动登记违规；物业巡检可手工登记，认定后扣押金 |
| 车辆迟到致垃圾点溢出 | 出场迟到判定 + 定时巡检（默认 60s），发布 RabbitMQ 事件，认定清运公司违约罚款 |
| 邻居投诉异味/扬尘 | 投诉受理→核查→认定违规→罚款，投诉责任链一键还原 |
| 混入生活垃圾 | 验收勾选 household 自动登记违规并扣款 |
| 称重数据异常 | 净重为负/与预估偏差超限/袋数不符/处置场复称差异，全部落异常单，挂起结算待人工裁决 |
| 费用争议 | 账单争议挂起→财务裁决调整金额（负数减收）→再核销 |
| 公共空间容量管控 | 接近阈值**不简单通过**：提示错峰堆放（需施工队确认）；满点拒绝；物业可设临时围挡按系数扩容 |
| 四方账单 | 业主清运费（按重+按袋+加袋附加）、施工队/业主罚款、清运公司按净重结算、物业垫付处置费台账 |
| 同一袋垃圾全程追踪 | 每动作落 `trace_event`，`/api/trace/bag/{bagCode}`、`/trace/registration/{id}`、`/trace/complaint/{id}` |

**单袋生命周期状态机**：
`REPORTED（已上报）→ ACCEPTED（物业核验收）→ SCHEDULED（已排入预约）→ LOADED（已装车）→ WEIGHED（已称重分摊）→ DISPOSED（处置场回执）`

---

## 二、一键启动（Docker Compose）

前置：安装 Docker 24+ 与 Docker Compose v2。

```bash
docker compose up -d --build
```

启动三个容器：

| 服务 | 地址 | 凭据 |
|---|---|---|
| 本应用 API | http://localhost:8080 | 见下方测试令牌 |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| OpenAPI 3 文档 | http://localhost:8080/v3/api-docs | — |
| MySQL 8 | localhost:3306 | 库 `reno_waste`，用户 `reno` / `reno123`，root `root123` |
| RabbitMQ 管理台 | http://localhost:15672 | guest / guest |

首次启动自动建表（JPA `ddl-auto=update`）并写入演示数据：
2 个垃圾点（P-A 容量 40 袋、P-B 容量 20 袋）、4 条门禁/限行规则、2 台车（京A12345、京B67891）、2 名司机、13 项计费参数。

停止与清理：

```bash
docker compose down            # 停止
docker compose down -v         # 停止并删除数据卷（清空数据）
```

---

## 三、本地开发启动（不使用 Docker）

需要 JDK 17、Maven 3.8+、MySQL 8（或 MariaDB 10.11+）。

```bash
# 1. 准备数据库
mysql -uroot -p <<'SQL'
CREATE DATABASE reno_waste DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'reno'@'%' IDENTIFIED BY 'reno123';
CREATE USER 'reno'@'localhost' IDENTIFIED BY 'reno123';
GRANT ALL ON reno_waste.* TO 'reno'@'%';
GRANT ALL ON reno_waste.* TO 'reno'@'localhost';
SQL

# 2. 启动（可选）RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

# 3. 运行应用（数据库连接走环境变量，默认 localhost:3306）
mvn spring-boot:run
```

**无 RabbitMQ 也可运行**：事件发布器检测到 broker 不可用时自动降级为同步处理（日志输出
`事件 xxx 同步降级处理`），业务不中断；接入 RabbitMQ 后自动切换为异步队列驱动，事件处理逻辑同一份代码。

数据库指向非默认地址时用环境变量覆盖：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、
`RABBIT_HOST`、`RABBIT_PORT`、`RABBIT_LISTENER_AUTOSTART`。

---

## 四、测试账号（请求头令牌）

本服务为演示用途采用静态令牌鉴权，所有 `/api/**` 请求需带请求头 `X-Auth-Token`：

| 角色 | X-Auth-Token | 主要权限 |
|---|---|---|
| 物业 | `property-token` | 登记审核、日报审核生成预约、违规认定、围挡、投诉处理、规则维护、物业看板 |
| 清运公司 | `company-token` | 派单预校验/派单、车辆出发、进出场称重、处置回执、公司看板 |
| 财务 | `finance-token` | 账单核销、争议裁决、计费参数、财务看板 |
| 施工队 | `crew-token` | 每日堆放上报 |
| 业主 | `owner-token` | 登记、临时加袋、投诉、费用争议 |

Swagger UI 右上角点 **Authorize** 填入令牌即可在线调试；`/api/public/health` 与文档页免鉴权。

---

## 五、验证方式

### 5.1 自动化测试（H2 内存库，无需外部中间件）

```bash
mvn test
```

- 单元测试：`AccessRuleServiceTest`（时间窗/跨零点/尾号限行/下一可进场时间）、
  `CapacityServiceTest`（容量通过/错峰/满点拒绝/围挡扩容）、`FeeServiceTest`（计费公式）。
- 端到端集成测试：`FullFlowIntegrationTest`（12 个有序用例，覆盖登记→追溯全链路、
  押金扣款、投诉追责、争议调整、称重偏差异常、三方看板、未认证 401）。

### 5.2 冒烟脚本（对已运行的真实环境，建议用 Docker Compose 启动后执行）

```bash
# 仅依赖 Python3 标准库，无需 pip 安装
python3 scripts/smoke.py                 # 默认 http://localhost:8080
python3 scripts/smoke.py http://localhost:8080
```

脚本对 15 个环节发起真实 HTTP 请求并断言结果：容量错峰、门禁拦截与建议时间、净重分摊到袋、
处置回执闭环、业主 513.0 元 / 公司 126.0 元 / 物业垫付 53.7 元账单、押金 3000→2500→2300、
争议减免后 483.0 元、投诉追责链等。房号每次运行自动带随机后缀，可重复执行。

### 5.3 手工快速验证（curl）

```bash
# 1) 未认证返回 401
curl -i http://localhost:8080/api/points

# 2) 容量试算（P-B 容量20，+15 袋达到 75% 阈值 → PEAK_SHIFT 错峰提示）
curl -s -X POST http://localhost:8080/api/points/capacity-check \
  -H 'X-Auth-Token: crew-token' -H 'Content-Type: application/json' \
  -d '{"pointId":2,"newBags":15}'

# 3) 派单门禁校验（22:00 不在作业时间窗 → errors 非空并给出下一可进场时间）
curl -s -X POST http://localhost:8080/api/appointments/dispatch/precheck \
  -H 'X-Auth-Token: company-token' -H 'Content-Type: application/json' \
  -d '{"appointmentIds":[1],"plate":"京A12345","plannedArrival":"2026-10-07T22:00:00"}'

# 4) 单袋全链路追溯（跑完冒烟/集成流程后用实际袋编码替换）
curl -s -H 'X-Auth-Token: owner-token' \
  http://localhost:8080/api/trace/bag/R1-D1-1
```

### 5.4 已实际执行的验证结论

- `mvn test`：**23 个用例全部通过**（11 个单元/规则用例 + 12 个端到端集成用例），BUILD SUCCESS。
- 使用本地 MariaDB 10.11 真实启动应用：建表、演示数据初始化成功，零 ERROR；
  在**无 RabbitMQ** 环境下确认事件同步降级，冒烟脚本 15 个环节全部断言通过。
- OpenAPI 3 文档可访问，共导出 59 个接口路径，Swagger UI 正常加载并支持令牌调试。
- 注：Dockerfile / docker-compose.yml 已按多阶段构建与健康检查编写；当前验证环境未安装
  Docker 守护进程，未在本机实际执行 `docker compose build`，镜像构建逻辑与本地 Maven 构建一致。

---

## 六、关键计费与规则参数（可在 `/api/fee-configs` 调整）

| 参数键 | 默认值 | 含义 |
|---|---|---|
| `disposal.price.per.kg` | 0.80 元/kg | 业主清运费按重单价 |
| `bag.fee` | 15 元/袋 | 按袋服务费 |
| `extra.bag.surcharge` | 10 元/袋 | 临时加袋附加费 |
| `company.settle.per.kg` | 0.35 元/kg | 清运公司按净重结算价 |
| `fine.fire.lane` / `fine.mixed.household` / `fine.illegal.location` | 500/300/200 | 各类违规罚款 |
| `fine.late` / `fine.spill` | 200/800 | 车辆迟到 / 迟到致溢出罚款 |
| `enclosure.capacity.factor` | 1.5 | 临时围挡期内容量扩容系数 |
| `weigh.deviation.limit` | 0.35 | 称重与预估偏差告警阈值 |
| `receipt.diff.limit` | 0.10 | 处置场复称差异告警阈值 |
| `bag.estimated.kg` | 25 | 标准袋预估重量（用于容量与称重校验） |

押金扣款规则：违规罚款优先从业主装修押金抵扣（施工队责任按管理约定向押金追偿），
押金不足时差额生成 `DEPOSIT_REPAY` 补缴单；清运公司责任计入其结算扣罚款账单。

---

## 七、RabbitMQ 事件设计

- 交换器：topic `waste.events`；队列：`q.appointment`（预约/派单/迟到）、`q.violation`、
  `q.weigh`（称重异常）、`q.complaint`。
- 典型事件：`waste.appointment.created`、`waste.appointment.extraBags`、`waste.dispatch.created`、
  `waste.trip.late`、`waste.weigh.anomaly`、`waste.violation.opened`、`waste.complaint.*`。
- 消费端 `EventProcessor` 同时是降级处理器：车辆迟到事件会自动认定清运公司违约，
  点位接近/达到上限时升级为“迟到致垃圾点溢出”并加重罚款。

---

## 八、工程结构

```
src/main/java/com/community/waste
├── domain/        领域枚举（状态机、违规/投诉/账单类型、容量决策、追溯事件类型）
├── entity/        JPA 实体（登记/日报/垃圾袋/预约/点位/车次/回执/违规/投诉/账单/争议/异常/追溯/规则）
├── repository/    Spring Data JPA 仓储
├── service/       业务服务（容量、门禁限行、登记、上报审核、派单、称重、回执、违规、投诉、争议、账单、看板、追溯）
├── mq/            RabbitMQ 拓扑、事件发布器（含同步降级）、事件处理器、超时巡检定时任务
├── web/           REST 控制器（16 组接口 + 首页/健康检查）
├── common/        令牌鉴权过滤器、全局异常、OpenAPI 配置、单号生成
└── bootstrap/     演示数据初始化
scripts/smoke.py  端到端冒烟脚本
```
