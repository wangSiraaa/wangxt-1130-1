# 大型农场植保施药审批系统

面向**农艺师、仓库管理员、飞手**三大角色的全流程植保作业管理系统，覆盖病虫害处方开具→农药出库（禁用农药拦截）→飞行作业（风速校验）→安全间隔期提醒的完整业务链路。

## ✨ 功能特性

### 🔬 业务流程
| 环节 | 角色 | 核心功能 | 校验规则 |
|------|------|---------|---------|
| 病虫害处方 | 农艺师 | 诊断、开处方、提交审批、驳回重提 | 明细农药禁用校验 |
| 处方审批 | 管理员/农艺师 | 通过/驳回 | 状态机流转 |
| 农药出库 | 仓管员 | 按处方生成出库单、选择批号、确认出库 | ①禁用农药禁止出库<br>②批号库存三重校验（存在/量足/未过期）<br>③库存自动扣减 |
| 飞行作业 | 飞手 | 天气登记、风速登记、作业参数、完成作业 | ①风速超限禁飞（农药级优先，通用5.0m/s兜底）<br>②雨天禁飞<br>③温湿度范围校验<br>④安全间隔期校验 |
| 间隔期管理 | 全员 | 自动生成提醒、三级预警、解禁通知 | 按农药+作物组合配置，回溯60天作业记录 |

### 📊 核心数据模型
11张核心数据表：
- **基础档案**: `sys_user`、`farm_plot`（地块）、`pesticide`（农药含禁用标记）、`pesticide_stock`（批号库存）
- **业务单据**: `prescription`(+detail)、`pesticide_outbound`(+detail)、`flight_operation`
- **安全配置**: `safety_interval_config`（农药+作物+间隔天数）、`safety_interval_reminder`（提醒日志）

### 💻 多端架构
```
┌───────────────────────────────────────────────────┐
│  Spring Boot 2.7 + MyBatis-Plus + MySQL 8.x (8080) │
└─────────────┬────────────────────┬──────────────────┘
              │ /api               │ /api
    ┌─────────▼───────┐  ┌────────▼────────┐
    │ Vue3 + Element  │  │ Vue3 + Vant H5  │
    │ PC管理台 (5173) │  │ 移动端 (5174)   │
    └─────────────────┘  └─────────────────┘
```

## 🚀 快速开始

### 环境要求
- JDK 1.8+
- MySQL 8.x 字符集 utf8mb4
- Node.js 16+ / npm 8+

### 第一步：初始化数据库
```bash
# 登录MySQL
mysql -u root -p

# 在MySQL命令行执行：
CREATE DATABASE IF NOT EXISTS plant_protection DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE plant_protection;
SOURCE /your-path/sql/schema.sql;  -- 11张表 + 完整测试数据
```

### 第二步：启动后端
```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
# 或者运行编译后的jar
java -jar target/plant-protection-1.0.0.jar
```
后端启动成功后访问：http://localhost:8080/api/common/dashboard

### 第三步：启动PC管理台（端口5173）
```bash
cd frontend/pc
npm install
npm run dev
```
访问：http://localhost:5173

### 第四步：启动移动端H5（端口5174）
```bash
cd frontend/mobile
npm install
npm run dev
# 手机访问时将 localhost 替换为电脑局域网IP
```
访问：http://localhost:5174 （建议使用 Chrome DevTools 移动端模式或手机扫码）

## 🔑 测试账号（均为 123456）

| 角色 | 用户名 | 登录端 |
|------|-------|--------|
| 管理员 | admin | PC端 |
| 农艺师 | agronomist1 | PC/移动端均可 |
| 仓库管理员 | warehouse1 | PC端 |
| 飞手 | pilot1 | 移动端为主，也可PC |

## 📋 业务流程演示

### 1️⃣ 标准流程（使用初始化数据）
1. **管理员**登录PC → 看到处方 `RX202606002`（已提交状态）→ 点击**通过**审批
2. **仓管员**登录 → 处方列表找到已审批处方 → 点「生成出库单」→ 出库详情页选择批号、选择飞手领用人 → 确认出库
3. **飞手**登录移动端 → 点「待作业」→ 点「开始作业」→ 填写风速、天气 → 安全预检（会显示间隔期/风速警告）→ 勾选责任确认 → 登记作业
4. 作业完成 → 自动生成 `BEFORE_INTERVAL / APPROACHING / EXPIRED` 三种类型的间隔期提醒
5. 任一端「间隔期提醒」页面查看剩余天数、危险/警告/提示三级预警，点击条目自动标记已读

### 2️⃣ 完整新建流程
1. 农艺师登录PC → 新增处方 → 选地块（东区一号地）→ 填写病虫害 → 添加农药明细（注意选择非禁用的，禁用农药PES004/PES008不会出现在可选列表）→ 保存并提交
2. 管理员审批通过
3. 仓管员按处方出库（选批号+领用人）
4. 飞手移动端登记风速（建议输入 **2.3** 测试通过，输入 **8.5** 测试风速超限拦截）
5. 作业完成后在提醒页面查看生成的间隔期提醒

### ⚠️ 边界场景演示
- **禁用农药出库**：处方明细中如果混入禁用农药（通过数据库修改测试），出库确认时 `BusinessValidator.validatePesticideNotForbidden()` 会抛出 400 异常
- **风速超限禁飞**：移动端作业登记风速超过农药级 `max_wind_speed` 或 5.0m/s 通用值 → 预检显示 DANGER 级别警告，勾选责任自负后才可强制提交
- **安全间隔期重复施药**：同一地块相同农药在间隔期内再次作业 → `BusinessValidator.validateSafetyInterval()` 校验失败

## 🧩 核心校验规则（BusinessValidator）

```java
// 5项业务规则集中校验，在出库/作业等关键节点链式调用
@Service public class BusinessValidator {
    void validatePesticideNotForbidden(Long pesticideId)      // 禁用农药=400
    void validatePesticideStock(id, batchNo, qty)             // 批号三重校验
    void validateWindSpeed(pesticideId, windSpeed)            // 农药级 > 通用5.0兜底
    void validateOperationWeather(weather, temp, humidity)    // 雨天/极端温湿度拦截
    void validateSafetyInterval(plotId, pesticideId, date)    // 回溯近60天
}
```

## 🗂 项目结构

```
.
├── backend/                                    # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/farm/plantprotection/
│       ├── PlantProtectionApplication.java
│       ├── common/                             # Result / 全局异常 / BusinessException
│       ├── config/                             # CORS / MyBatis-Plus / MetaObjectHandler
│       ├── entity/                             # 11个实体类
│       ├── mapper/                             # 11个BaseMapper
│       └── service/                            # 4个业务Service + BusinessValidator
│           └── PrescriptionService / PesticideOutboundService
│           └── FlightOperationService / CommonService
├── frontend/pc/                                # Vue3 Element Plus PC端
│   ├── src/
│   │   ├── views/prescription/                 # 处方列表/创建/详情
│   │   ├── views/outbound/                     # 出库列表/详情
│   │   ├── views/flight/                       # 飞行作业
│   │   ├── views/basic/                        # 地块/农药/库存档案
│   │   ├── views/Dashboard / Login / Reminders
│   │   └── layout / router / api / utils
├── frontend/mobile/                            # Vue3 Vant H5移动端
│   └── src/views/
│       ├── Home / Login / Profile
│       ├── Tasks.vue（待作业任务列表）
│       ├── Operation.vue（核心：飞行作业登记+安全预检）
│       ├── Warnings.vue（间隔期提醒中心）
│       └── Prescriptions.vue / PrescriptionCreate.vue / PrescriptionDetail.vue
└── sql/schema.sql                              # 建表脚本 + 完整测试数据
```

## 🔌 核心 REST API 清单

| 模块 | 方法 | 路径 | 说明 |
|-----|-----|------|------|
| 处方 | POST | `/prescription/create` | 创建处方（含明细） |
| 处方 | POST | `/prescription/submit/{id}` | 提交审批 |
| 处方 | POST | `/prescription/approve/{id}` | 审批通过/驳回 |
| 处方 | GET  | `/prescription/page` | 分页查询（支持状态/地块/农艺师） |
| 出库 | POST | `/outbound/create-from-prescription` | 从已审批处方生成出库单 |
| 出库 | POST | `/outbound/confirm/{id}` | 确认出库（**触发禁用校验+库存扣减**） |
| 出库 | GET  | `/outbound/pending-operations/{pilotId}` | 飞手端待作业列表 |
| 作业 | POST | `/flight/pre-check` | 作业安全预检（风速/间隔期综合警告）|
| 作业 | POST | `/flight/create` | 创建作业记录（**三重校验：风速+天气+间隔期**） |
| 作业 | POST | `/flight/complete/{id}` | 完成作业（**自动生成间隔期提醒**） |
| 作业 | GET  | `/flight/plot-warnings` | 查询活动中的地块警告 |
| 提醒 | GET  | `/flight/reminders` | 分页查询间隔期提醒 |
| 提醒 | POST | `/flight/reminders/{id}/read` | 标记提醒已读 |
| 公共 | GET  | `/common/dashboard` | 首页统计 |
| 公共 | GET  | `/common/plots` / `/common/pesticides` / `/common/users` | 基础数据 |

## 🛠 关键技术点说明

### 1. 状态机设计
- 处方：`DRAFT(草稿) → SUBMITTED(已提交) → APPROVED(已审批) / REJECTED(已驳回) → COMPLETED(已出库完成)`
- 出库：`PENDING(待出库) → OUTBOUND(已出库) → COMPLETED(作业完成)`
- 作业：`PENDING(待飞) / IN_PROGRESS(飞行中) → COMPLETED(已完成)`

### 2. 安全间隔期实现
- **源头配置**：`safety_interval_config` 表按 农药ID + 作物 维度配置间隔天数
- **作业时校验**：提交作业时调用 `validateSafetyInterval` 回溯近60天作业记录
- **完成时生成**：`FlightOperationService.generateSafetyIntervalReminders()` 扫描出库单所有农药，计算 `lastOperationDate + 间隔天数 = safeEndDate`
- **三级提醒**：
  - `BEFORE_INTERVAL`（剩余>3天，INFO级 蓝色）
  - `APPROACHING`（剩余≤3天，WARNING级 橙色）
  - `EXPIRED`（剩余≤0即已到期，DANGER级 红色，禁采收警告）

### 3. 风速校验优先级
```
农药表max_wind_speed（个性化配置）
          ↓ 如果为NULL
    通用限制 5.0 m/s 兜底
```
示例：草甘膦异丙胺盐 `max_wind_speed = 3.0` 比通用值更严格；若未配置则5.0m/s约对应3-4级风，是植保无人机常规作业上限。

### 4. 库存批号三重校验
```
① 批号是否存在于pesticide_stock表
② currentQuantity >= 实际出库量
③ 有效期expiryDate >= 今天 且 status != 1(过期)
```
三重校验任意一项失败，`BusinessException` 携带具体信息返回前端。

---
© 2024 Farm Plant Protection System · 全栈一体化植保作业管理解决方案
