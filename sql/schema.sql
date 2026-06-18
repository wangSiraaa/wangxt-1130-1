-- =============================================
-- 大型农场植保施药审批系统 数据库脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS plant_protection DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE plant_protection;

-- =============================================
-- 1. 用户表
-- =============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password        VARCHAR(100)    NOT NULL DEFAULT '123456' COMMENT '密码',
    real_name       VARCHAR(50)     NOT NULL COMMENT '真实姓名',
    role_code       VARCHAR(30)     NOT NULL COMMENT '角色: AGRONOMIST-农艺师 WAREHOUSE-仓库 PILOT-飞手 ADMIN-管理员',
    phone           VARCHAR(20)             COMMENT '手机号',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =============================================
-- 2. 地块档案表
-- =============================================
DROP TABLE IF EXISTS farm_plot;
CREATE TABLE farm_plot (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plot_code       VARCHAR(30)     NOT NULL COMMENT '地块编号',
    plot_name       VARCHAR(100)    NOT NULL COMMENT '地块名称',
    location        VARCHAR(255)            COMMENT '地块位置',
    area            DECIMAL(10,2)   NOT NULL COMMENT '地块面积(亩)',
    crop_type       VARCHAR(50)             COMMENT '作物类型',
    crop_variety    VARCHAR(50)             COMMENT '作物品种',
    planting_date   DATE                    COMMENT '种植日期',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-在用 0-停用',
    longitude       DECIMAL(12,8)           COMMENT '经度',
    latitude        DECIMAL(12,8)           COMMENT '纬度',
    remark          VARCHAR(500)            COMMENT '备注',
    create_by       BIGINT                  COMMENT '创建人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plot_code (plot_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地块档案表';

-- =============================================
-- 3. 农药档案表
-- =============================================
DROP TABLE IF EXISTS pesticide;
CREATE TABLE pesticide (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pesticide_code      VARCHAR(30)     NOT NULL COMMENT '农药编码',
    pesticide_name      VARCHAR(100)    NOT NULL COMMENT '农药名称',
    common_name         VARCHAR(100)            COMMENT '通用名称',
    registration_no     VARCHAR(50)             COMMENT '农药登记证号',
    manufacturer        VARCHAR(100)            COMMENT '生产厂家',
    pesticide_type      VARCHAR(30)             COMMENT '农药类型: 杀虫剂/杀菌剂/除草剂/植物生长调节剂',
    toxicity            VARCHAR(20)             COMMENT '毒性等级: 微毒/低毒/中毒/高毒/剧毒',
    specification       VARCHAR(50)             COMMENT '规格/剂型',
    unit                VARCHAR(10)             COMMENT '计量单位',
    is_forbidden        TINYINT         NOT NULL DEFAULT 0 COMMENT '是否禁用: 0-否 1-是',
    forbidden_reason    VARCHAR(255)            COMMENT '禁用原因',
    safety_interval     INT                     COMMENT '安全间隔期(天)',
    max_wind_speed      DECIMAL(5,2)            COMMENT '最大允许风速(m/s)',
    applicable_crops    VARCHAR(255)            COMMENT '适用作物',
    control_objects     VARCHAR(255)            COMMENT '防治对象',
    dosage              VARCHAR(100)            COMMENT '推荐用量',
    usage_method        VARCHAR(255)            COMMENT '使用方法',
    precautions         VARCHAR(500)            COMMENT '注意事项',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    remark              VARCHAR(500)            COMMENT '备注',
    create_by           BIGINT                  COMMENT '创建人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pesticide_code (pesticide_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农药档案表';

-- =============================================
-- 4. 农药库存表
-- =============================================
DROP TABLE IF EXISTS pesticide_stock;
CREATE TABLE pesticide_stock (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pesticide_id    BIGINT          NOT NULL COMMENT '农药ID',
    batch_no        VARCHAR(50)     NOT NULL COMMENT '批号',
    production_date DATE                    COMMENT '生产日期',
    expiry_date     DATE                    COMMENT '有效期至',
    quantity        DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '库存数量',
    unit            VARCHAR(10)             COMMENT '单位',
    warehouse_id    BIGINT                  COMMENT '仓库ID',
    warehouse_name  VARCHAR(100)            COMMENT '仓库名称',
    location_code   VARCHAR(50)             COMMENT '库位号',
    remark          VARCHAR(500)            COMMENT '备注',
    create_by       BIGINT                  COMMENT '创建人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch (pesticide_id, batch_no),
    KEY idx_pesticide_id (pesticide_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农药库存表';

-- =============================================
-- 5. 病虫害处方单表
-- =============================================
DROP TABLE IF EXISTS prescription;
CREATE TABLE prescription (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    prescription_no     VARCHAR(30)     NOT NULL COMMENT '处方单号',
    plot_id             BIGINT          NOT NULL COMMENT '地块ID',
    plot_name           VARCHAR(100)    NOT NULL COMMENT '地块名称(冗余)',
    pest_type           VARCHAR(100)    NOT NULL COMMENT '病虫害类型',
    pest_name           VARCHAR(100)    NOT NULL COMMENT '病虫害名称',
    severity            VARCHAR(20)             COMMENT '严重程度: 轻/中/重',
    occurrence_area     DECIMAL(10,2)           COMMENT '发生面积(亩)',
    symptom_description TEXT                    COMMENT '症状描述',
    diagnosis_basis     VARCHAR(500)            COMMENT '诊断依据',
    treatment_scheme    TEXT                    COMMENT '防治方案',
    expected_effect     VARCHAR(500)            COMMENT '预期效果',
    prescription_date   DATE            NOT NULL COMMENT '开方日期',
    agronomist_id       BIGINT          NOT NULL COMMENT '开方农艺师ID',
    agronomist_name     VARCHAR(50)     NOT NULL COMMENT '开方农艺师(冗余)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿 SUBMITTED-已提交 APPROVED-已审批 REJECTED-已驳回 COMPLETED-已完成',
    approve_by          BIGINT                  COMMENT '审批人',
    approve_time        DATETIME                COMMENT '审批时间',
    approve_remark      VARCHAR(500)            COMMENT '审批意见',
    remark              VARCHAR(500)            COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_prescription_no (prescription_no),
    KEY idx_plot_id (plot_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病虫害处方单表';

-- =============================================
-- 6. 处方明细表
-- =============================================
DROP TABLE IF EXISTS prescription_detail;
CREATE TABLE prescription_detail (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    prescription_id     BIGINT          NOT NULL COMMENT '处方ID',
    pesticide_id        BIGINT          NOT NULL COMMENT '农药ID',
    pesticide_code      VARCHAR(30)     NOT NULL COMMENT '农药编码(冗余)',
    pesticide_name      VARCHAR(100)    NOT NULL COMMENT '农药名称(冗余)',
    dosage_per_mu       DECIMAL(12,4)   NOT NULL COMMENT '每亩用量',
    unit                VARCHAR(10)     NOT NULL COMMENT '单位',
    total_quantity      DECIMAL(12,2)   NOT NULL COMMENT '总用量',
    dilution_ratio      VARCHAR(50)             COMMENT '稀释比例',
    usage_method        VARCHAR(255)            COMMENT '使用方法',
    spraying_times      INT                     COMMENT '喷施次数',
    spraying_interval   INT                     COMMENT '喷施间隔(天)',
    remark              VARCHAR(500)            COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_prescription_id (prescription_id),
    KEY idx_pesticide_id (pesticide_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细表';

-- =============================================
-- 7. 农药出库单表
-- =============================================
DROP TABLE IF EXISTS pesticide_outbound;
CREATE TABLE pesticide_outbound (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    outbound_no         VARCHAR(30)     NOT NULL COMMENT '出库单号',
    prescription_id     BIGINT          NOT NULL COMMENT '关联处方ID',
    prescription_no     VARCHAR(30)     NOT NULL COMMENT '处方单号(冗余)',
    plot_id             BIGINT          NOT NULL COMMENT '地块ID',
    plot_name           VARCHAR(100)    NOT NULL COMMENT '地块名称(冗余)',
    outbound_type       VARCHAR(20)     NOT NULL DEFAULT 'PRESCRIPTION' COMMENT '出库类型: PRESCRIPTION-处方出库 OTHER-其他出库',
    outbound_date       DATE            NOT NULL COMMENT '出库日期',
    warehouse_keeper_id BIGINT          NOT NULL COMMENT '仓库管理员ID',
    warehouse_keeper_name VARCHAR(50)   NOT NULL COMMENT '仓库管理员(冗余)',
    receiver_id         BIGINT                  COMMENT '领用人ID(飞手)',
    receiver_name       VARCHAR(50)             COMMENT '领用人(冗余)',
    total_amount        DECIMAL(14,2)           COMMENT '总金额',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待出库 OUTBOUND-已出库 CANCELLED-已取消',
    outbound_time       DATETIME                COMMENT '出库时间',
    remark              VARCHAR(500)            COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbound_no (outbound_no),
    KEY idx_prescription_id (prescription_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农药出库单表';

-- =============================================
-- 8. 农药出库明细表
-- =============================================
DROP TABLE IF EXISTS pesticide_outbound_detail;
CREATE TABLE pesticide_outbound_detail (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    outbound_id         BIGINT          NOT NULL COMMENT '出库单ID',
    prescription_detail_id BIGINT                COMMENT '处方明细ID',
    pesticide_id        BIGINT          NOT NULL COMMENT '农药ID',
    pesticide_code      VARCHAR(30)     NOT NULL COMMENT '农药编码(冗余)',
    pesticide_name      VARCHAR(100)    NOT NULL COMMENT '农药名称(冗余)',
    batch_no            VARCHAR(50)     NOT NULL COMMENT '批号',
    quantity            DECIMAL(12,2)   NOT NULL COMMENT '出库数量',
    unit                VARCHAR(10)     NOT NULL COMMENT '单位',
    unit_price          DECIMAL(12,4)           COMMENT '单价',
    total_price         DECIMAL(14,2)           COMMENT '小计金额',
    stock_id            BIGINT                  COMMENT '库存ID',
    remark              VARCHAR(500)            COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_outbound_id (outbound_id),
    KEY idx_pesticide_id (pesticide_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农药出库明细表';

-- =============================================
-- 9. 飞行作业记录表
-- =============================================
DROP TABLE IF EXISTS flight_operation;
CREATE TABLE flight_operation (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    operation_no        VARCHAR(30)     NOT NULL COMMENT '作业编号',
    outbound_id         BIGINT          NOT NULL COMMENT '关联出库单ID',
    outbound_no         VARCHAR(30)     NOT NULL COMMENT '出库单号(冗余)',
    prescription_id     BIGINT          NOT NULL COMMENT '处方ID',
    plot_id             BIGINT          NOT NULL COMMENT '地块ID',
    plot_name           VARCHAR(100)    NOT NULL COMMENT '地块名称(冗余)',
    pilot_id            BIGINT          NOT NULL COMMENT '飞手ID',
    pilot_name          VARCHAR(50)     NOT NULL COMMENT '飞手姓名(冗余)',
    operation_date      DATE            NOT NULL COMMENT '作业日期',
    start_time          DATETIME                COMMENT '开始时间',
    end_time            DATETIME                COMMENT '结束时间',
    flight_duration     INT                     COMMENT '飞行时长(分钟)',
    operation_area      DECIMAL(10,2)   NOT NULL COMMENT '作业面积(亩)',
    drone_no            VARCHAR(50)             COMMENT '无人机编号',
    weather             VARCHAR(50)             COMMENT '天气: 晴/多云/阴/小雨 等',
    temperature         DECIMAL(5,2)            COMMENT '温度(℃)',
    humidity            DECIMAL(5,2)            COMMENT '湿度(%)',
    wind_direction      VARCHAR(20)             COMMENT '风向',
    wind_speed          DECIMAL(5,2)            COMMENT '风速(m/s)',
    wind_level          VARCHAR(20)             COMMENT '风力等级',
    spraying_volume     DECIMAL(12,2)           COMMENT '喷施药液量(L)',
    pesticide_usage     TEXT                    COMMENT '农药使用情况(JSON存储)',
    flight_height       DECIMAL(8,2)            COMMENT '飞行高度(m)',
    flight_speed        DECIMAL(8,2)            COMMENT '飞行速度(m/s)',
    spray_width         DECIMAL(8,2)            COMMENT '喷幅(m)',
    operation_status    VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '作业状态: IN_PROGRESS-进行中 COMPLETED-已完成 PAUSED-已暂停 CANCELLED-已取消',
    safety_check        TINYINT         NOT NULL DEFAULT 0 COMMENT '安全检查: 0-未通过 1-已通过',
    safety_remark       VARCHAR(500)            COMMENT '安全检查备注',
    problem_description TEXT                    COMMENT '问题描述',
    solution            TEXT                    COMMENT '解决措施',
    effect_evaluation   VARCHAR(500)            COMMENT '效果评价',
    photo_urls          TEXT                    COMMENT '作业照片(JSON数组)',
    video_url           VARCHAR(500)            COMMENT '作业视频',
    remark              VARCHAR(500)            COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_operation_no (operation_no),
    KEY idx_outbound_id (outbound_id),
    KEY idx_plot_id (plot_id),
    KEY idx_pilot_id (pilot_id),
    KEY idx_operation_date (operation_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞行作业记录表';

-- =============================================
-- 10. 安全间隔期配置表
-- =============================================
DROP TABLE IF EXISTS safety_interval_config;
CREATE TABLE safety_interval_config (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_name         VARCHAR(100)    NOT NULL COMMENT '配置名称',
    pesticide_id        BIGINT          NOT NULL COMMENT '农药ID',
    pesticide_name      VARCHAR(100)    NOT NULL COMMENT '农药名称(冗余)',
    crop_type           VARCHAR(50)     NOT NULL COMMENT '作物类型',
    interval_days       INT             NOT NULL COMMENT '安全间隔期(天)',
    max_wind_speed      DECIMAL(5,2)            COMMENT '最大允许风速(m/s)',
    min_temperature     DECIMAL(5,2)            COMMENT '最低温度(℃)',
    max_temperature     DECIMAL(5,2)            COMMENT '最高温度(℃)',
    min_humidity        DECIMAL(5,2)            COMMENT '最低湿度(%)',
    max_humidity        DECIMAL(5,2)            COMMENT '最高湿度(%)',
    forbidden_rain      TINYINT         NOT NULL DEFAULT 1 COMMENT '雨天禁止作业: 0-否 1-是',
    description         VARCHAR(500)            COMMENT '说明',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-启用 0-停用',
    create_by           BIGINT                  COMMENT '创建人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pesticide_crop (pesticide_id, crop_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全间隔期配置表';

-- =============================================
-- 11. 间隔期提醒日志表
-- =============================================
DROP TABLE IF EXISTS safety_interval_reminder;
CREATE TABLE safety_interval_reminder (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    reminder_no         VARCHAR(30)     NOT NULL COMMENT '提醒单号',
    plot_id             BIGINT          NOT NULL COMMENT '地块ID',
    plot_name           VARCHAR(100)    NOT NULL COMMENT '地块名称(冗余)',
    pesticide_id        BIGINT          NOT NULL COMMENT '农药ID',
    pesticide_name      VARCHAR(100)    NOT NULL COMMENT '农药名称(冗余)',
    last_operation_id   BIGINT          NOT NULL COMMENT '上次作业ID',
    last_operation_date DATE            NOT NULL COMMENT '上次作业日期',
    safe_end_date       DATE            NOT NULL COMMENT '安全期结束日期(可采收日期)',
    interval_days       INT             NOT NULL COMMENT '间隔天数',
    remaining_days      INT             NOT NULL COMMENT '剩余天数',
    reminder_type       VARCHAR(20)     NOT NULL COMMENT '提醒类型: BEFORE_INTERVAL-间隔期内(禁采) APPROACHING-临近到期 EXPIRED-已到期可采收',
    reminder_level      VARCHAR(20)     NOT NULL COMMENT '提醒级别: INFO-提示 WARNING-警告 DANGER-危险',
    remind_content      VARCHAR(500)    NOT NULL COMMENT '提醒内容',
    is_read             TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已读: 0-否 1-是',
    read_time           DATETIME                COMMENT '阅读时间',
    read_by             BIGINT                  COMMENT '阅读人',
    notify_users        VARCHAR(500)            COMMENT '通知用户ID列表(逗号分隔)',
    reminder_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提醒时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reminder_no (reminder_no),
    KEY idx_plot_id (plot_id),
    KEY idx_safe_end_date (safe_end_date),
    KEY idx_reminder_type (reminder_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全间隔期提醒日志表';

-- =============================================
-- 初始化数据
-- =============================================

-- 初始化用户
INSERT INTO sys_user (username, real_name, role_code, phone) VALUES
('admin', '系统管理员', 'ADMIN', '13800000000'),
('agronomist1', '张农艺', 'AGRONOMIST', '13800000001'),
('agronomist2', '李艺师', 'AGRONOMIST', '13800000002'),
('warehouse1', '王仓库', 'WAREHOUSE', '13800000003'),
('warehouse2', '赵保管', 'WAREHOUSE', '13800000004'),
('pilot1', '陈飞手', 'PILOT', '13800000005'),
('pilot2', '刘飞行员', 'PILOT', '13800000006');

-- 初始化地块
INSERT INTO farm_plot (plot_code, plot_name, location, area, crop_type, crop_variety, planting_date, longitude, latitude, remark, create_by) VALUES
('P001', '东区一号地', '农场东区A区', 120.50, '水稻', '籼稻9311', '2026-04-15', 116.407400, 39.904200, '土壤肥沃，灌溉便利', 1),
('P002', '东区二号地', '农场东区B区', 85.30, '水稻', '粳稻日本晴', '2026-04-18', 116.408500, 39.905300, '靠近水渠', 1),
('P003', '西区一号地', '农场西区C区', 150.00, '小麦', '济麦22', '2026-03-10', 116.405000, 39.906000, '沙壤土', 1),
('P004', '西区二号地', '农场西区D区', 95.80, '玉米', '登海605', '2026-05-01', 116.404000, 39.907000, '高产地块', 1),
('P005', '南区果园', '农场南区E区', 60.20, '苹果', '红富士', '2023-03-20', 116.409000, 39.903000, '多年生果树', 1);

-- 初始化农药档案
INSERT INTO pesticide (pesticide_code, pesticide_name, common_name, registration_no, manufacturer, pesticide_type, toxicity, specification, unit, is_forbidden, safety_interval, max_wind_speed, applicable_crops, control_objects, dosage, usage_method, precautions, create_by) VALUES
('PES001', '吡虫啉', '吡虫啉', 'PD20040001', '江苏某农药厂', '杀虫剂', '低毒', '10%可湿性粉剂', 'kg', 0, 14, 5.0, '水稻/小麦/蔬菜', '蚜虫/飞虱/蓟马', '15-20g/亩', '兑水喷雾', '避免强阳光下使用，蜜蜂作物花期禁用', 1),
('PES002', '三环唑', '三环唑', 'PD20050002', '浙江某化工', '杀菌剂', '中毒', '75%可湿性粉剂', 'kg', 0, 21, 4.5, '水稻', '稻瘟病', '20-25g/亩', '兑水喷雾', '避免与强酸强碱混用', 1),
('PES003', '草甘膦异丙胺盐', '草甘膦', 'PD20060003', '山东某农化', '除草剂', '低毒', '41%水剂', 'L', 0, 7, 5.0, '非耕地/果园行间', '一年生和多年生杂草', '200-300ml/亩', '定向喷雾', '严禁喷洒到作物上，施药后4小时内遇雨需重喷', 1),
('PES004', '甲胺磷', '甲胺磷', 'PD20070004', '某老农药厂', '杀虫剂', '高毒', '50%乳油', 'kg', 1, 30, 3.0, '棉花/水稻', '棉铃虫/二化螟', '50-100ml/亩', '兑水喷雾', '高毒农药，国家已禁用，禁止在蔬菜、果树、茶叶上使用', 1),
('PES005', '多菌灵', '多菌灵', 'PD20080005', '安徽某药业', '杀菌剂', '低毒', '50%可湿性粉剂', 'kg', 0, 15, 5.0, '多种作物', '赤霉病/纹枯病/白粉病', '40-60g/亩', '兑水喷雾', '不能与铜制剂混用', 1),
('PES006', '氯氰菊酯', '氯氰菊酯', 'PD20090006', '广东某化工', '杀虫剂', '中毒', '10%乳油', 'L', 0, 10, 4.0, '蔬菜/果树/棉花', '菜青虫/蚜虫/棉铃虫', '30-50ml/亩', '兑水喷雾', '对鱼、蜜蜂高毒，注意环境保护', 1),
('PES007', '戊唑醇', '戊唑醇', 'PD20100007', '德国拜耳', '杀菌剂', '低毒', '43%悬浮剂', 'L', 0, 25, 5.0, '小麦/水稻/果树', '白粉病/锈病/斑点落叶病', '15-25ml/亩', '兑水喷雾', '严格按照推荐剂量使用', 1),
('PES008', '敌敌畏', '敌敌畏', 'PD20110008', '河北某农药厂', '杀虫剂', '中毒', '80%乳油', 'kg', 1, 28, 3.5, '大田作物', '多种害虫', '50-100ml/亩', '喷雾/熏蒸', '高毒有机磷，已在部分作物上禁用', 1);

-- 初始化农药库存
INSERT INTO pesticide_stock (pesticide_id, batch_no, production_date, expiry_date, quantity, unit, warehouse_name, location_code, remark, create_by) VALUES
(1, 'B20260101', '2026-01-01', '2028-01-01', 500.00, 'kg', '一号仓库', 'A-01-01', '吡虫啉库存充足', 1),
(1, 'B20260215', '2026-02-15', '2028-02-15', 300.00, 'kg', '一号仓库', 'A-01-02', '2月新入库批次', 1),
(2, 'B20260120', '2026-01-20', '2027-07-20', 250.00, 'kg', '一号仓库', 'A-02-01', '三环唑库存', 1),
(3, 'B20260301', '2026-03-01', '2027-09-01', 400.00, 'L', '二号仓库', 'B-01-01', '草甘膦库存', 1),
(4, 'B20250601', '2025-06-01', '2026-12-31', 100.00, 'kg', '隔离仓库', 'C-01-01', '禁用农药，禁止出库', 1),
(5, 'B20260201', '2026-02-01', '2028-02-01', 350.00, 'kg', '一号仓库', 'A-03-01', '多菌灵库存', 1),
(6, 'B20260315', '2026-03-15', '2027-09-15', 180.00, 'L', '一号仓库', 'A-04-01', '氯氰菊酯库存', 1),
(7, 'B20260401', '2026-04-01', '2028-04-01', 150.00, 'L', '二号仓库', 'B-02-01', '戊唑醇库存', 1),
(8, 'B20250501', '2025-05-01', '2026-11-01', 80.00, 'kg', '隔离仓库', 'C-02-01', '禁用农药，禁止出库', 1);

-- 初始化安全间隔期配置
INSERT INTO safety_interval_config (config_name, pesticide_id, pesticide_name, crop_type, interval_days, max_wind_speed, min_temperature, max_temperature, min_humidity, max_humidity, forbidden_rain, description, create_by) VALUES
('吡虫啉-水稻', 1, '吡虫啉', '水稻', 14, 5.0, 15, 35, 30, 80, 1, '水稻田最后一次施药距采收至少14天', 1),
('三环唑-水稻', 2, '三环唑', '水稻', 21, 4.5, 18, 35, 40, 85, 1, '稻瘟病防治，安全间隔期21天', 1),
('多菌灵-小麦', 5, '多菌灵', '小麦', 15, 5.0, 10, 32, 30, 80, 1, '小麦赤霉病防治，安全间隔期15天', 1),
('氯氰菊酯-蔬菜', 6, '氯氰菊酯', '蔬菜', 10, 4.0, 15, 32, 30, 75, 1, '蔬菜害虫防治，采前10天禁用', 1),
('戊唑醇-果树', 7, '戊唑醇', '苹果', 25, 5.0, 12, 32, 35, 80, 1, '苹果斑点落叶病防治，安全间隔期25天', 1),
('吡虫啉-小麦', 1, '吡虫啉', '小麦', 14, 5.0, 10, 35, 30, 80, 1, '小麦蚜虫防治，安全间隔期14天', 1);

-- 初始化示例处方单
INSERT INTO prescription (prescription_no, plot_id, plot_name, pest_type, pest_name, severity, occurrence_area, symptom_description, diagnosis_basis, treatment_scheme, expected_effect, prescription_date, agronomist_id, agronomist_name, status, approve_by, approve_time, remark) VALUES
('RX202606001', 1, '东区一号地', '虫害', '稻飞虱', '中', 50.00, '叶片发黄，基部有大量飞虱活动，部分田块出现冒穿', '田间虫口密度调查，百丛虫量达1500头', '使用吡虫啉10%可湿性粉剂20g/亩，兑水50kg均匀喷雾，7天后复查', '预计防治效果90%以上', '2026-06-10', 2, '张农艺', 'APPROVED', 1, '2026-06-10 09:30:00', '建议在早晨或傍晚施药'),
('RX202606002', 1, '东区一号地', '病害', '稻瘟病', '轻', 30.00, '叶部出现梭形病斑，中央灰白色边缘褐色', '典型稻瘟病症状，气候条件适宜病害发展', '三环唑75%可湿性粉剂25g/亩，兑水50kg均匀喷雾', '控制病害扩展，保护功能叶片', '2026-06-12', 2, '张农艺', 'APPROVED', 1, '2026-06-12 10:15:00', NULL),
('RX202606003', 3, '西区一号地', '虫害', '麦蚜', '中', 80.00, '穗部和上部叶片蚜虫聚集，分泌蜜露', '百株蚜量超过500头，达到防治指标', '吡虫啉20g/亩 + 多菌灵50g/亩混合喷雾', '控制蚜虫危害，兼防赤霉病', '2026-06-13', 3, '李艺师', 'SUBMITTED', NULL, NULL, '待审批');

-- 初始化处方明细
INSERT INTO prescription_detail (prescription_id, pesticide_id, pesticide_code, pesticide_name, dosage_per_mu, unit, total_quantity, dilution_ratio, usage_method, spraying_times, spraying_interval, remark) VALUES
(1, 1, 'PES001', '吡虫啉', 20.0000, 'g/亩', 10.00, '1:2500', '均匀喷雾，重点喷施基部', 1, NULL, '选用批次B20260101'),
(2, 2, 'PES002', '三环唑', 25.0000, 'g/亩', 7.50, '1:2000', '整株均匀喷雾', 1, NULL, '注意避开水稻扬花期'),
(3, 1, 'PES001', '吡虫啉', 20.0000, 'g/亩', 16.00, '1:2500', '均匀喷雾', 1, NULL, NULL),
(3, 5, 'PES005', '多菌灵', 50.0000, 'g/亩', 40.00, '1:1000', '均匀喷雾', 1, NULL, '与吡虫啉现混现用');

-- 初始化示例出库单
INSERT INTO pesticide_outbound (outbound_no, prescription_id, prescription_no, plot_id, plot_name, outbound_type, outbound_date, warehouse_keeper_id, warehouse_keeper_name, receiver_id, receiver_name, total_amount, status, outbound_time, remark) VALUES
('OUT202606001', 1, 'RX202606001', 1, '东区一号地', 'PRESCRIPTION', '2026-06-11', 4, '王仓库', 6, '陈飞手', NULL, 'OUTBOUND', '2026-06-11 08:30:00', '按处方如数发放'),
('OUT202606002', 2, 'RX202606002', 1, '东区一号地', 'PRESCRIPTION', '2026-06-13', 4, '王仓库', 6, '陈飞手', NULL, 'OUTBOUND', '2026-06-13 07:45:00', '已核对批号和有效期');

-- 初始化出库明细
INSERT INTO pesticide_outbound_detail (outbound_id, prescription_detail_id, pesticide_id, pesticide_code, pesticide_name, batch_no, quantity, unit, stock_id, remark) VALUES
(1, 1, 1, 'PES001', '吡虫啉', 'B20260101', 10.00, 'kg', 1, '按处方发放'),
(2, 2, 2, 'PES002', '三环唑', 'B20260120', 7.50, 'kg', 3, '按处方发放');

-- 初始化示例飞行作业
INSERT INTO flight_operation (operation_no, outbound_id, outbound_no, prescription_id, plot_id, plot_name, pilot_id, pilot_name, operation_date, start_time, end_time, flight_duration, operation_area, drone_no, weather, temperature, humidity, wind_direction, wind_speed, wind_level, spraying_volume, flight_height, flight_speed, spray_width, operation_status, safety_check, safety_remark, effect_evaluation, remark) VALUES
('FLY202606001', 1, 'OUT202606001', 1, 1, '东区一号地', 6, '陈飞手', '2026-06-11', '2026-06-11 09:00:00', '2026-06-11 10:25:00', 85, 50.00, 'UAV-001', '晴', 26.5, 65.0, '东风', 3.2, '2级', 2500.00, 2.5, 6.0, 3.0, 'COMPLETED', 1, '风速、天气符合要求，已安全检查', '作业质量良好，喷施均匀', '按计划完成作业'),
('FLY202606002', 2, 'OUT202606002', 2, 1, '东区一号地', 6, '陈飞手', '2026-06-13', '2026-06-13 16:30:00', '2026-06-13 17:12:00', 42, 30.00, 'UAV-001', '多云', 28.0, 70.0, '东南风', 2.8, '2级', 1500.00, 2.5, 5.5, 3.0, 'COMPLETED', 1, '风力适宜，无降雨预报', '喷施覆盖良好', '避开中午高温时段作业');

-- 初始化间隔期提醒
INSERT INTO safety_interval_reminder (reminder_no, plot_id, plot_name, pesticide_id, pesticide_name, last_operation_id, last_operation_date, safe_end_date, interval_days, remaining_days, reminder_type, reminder_level, remind_content, notify_users) VALUES
('REM202606001', 1, '东区一号地', 1, '吡虫啉', 1, '2026-06-11', '2026-06-25', 14, 7, 'BEFORE_INTERVAL', 'WARNING', '东区一号地于2026-06-11喷施吡虫啉，安全间隔期14天，当前距可采收日期还有7天，禁止采收！', '2,3,6,7'),
('REM202606002', 1, '东区一号地', 2, '三环唑', 2, '2026-06-13', '2026-07-04', 21, 16, 'BEFORE_INTERVAL', 'DANGER', '东区一号地于2026-06-13喷施三环唑，安全间隔期21天，距离可采收日期还有16天，严禁采收！', '2,3,6,7');
