CREATE DATABASE IF NOT EXISTS product_select DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE product_select;

-- ============================================================
-- 商品表
-- ============================================================
CREATE TABLE IF NOT EXISTS `product` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `num_iid`         VARCHAR(100) DEFAULT NULL COMMENT '1688商品ID(去重用)',
  `title`           VARCHAR(500) NOT NULL COMMENT '商品标题',
  `price`           DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
  `agent_price`     DECIMAL(10,2) DEFAULT NULL COMMENT '代发价',
  `min_num`         INT DEFAULT NULL COMMENT '起批量',
  `location`        VARCHAR(200) DEFAULT NULL COMMENT '发货地',
  `sales`           BIGINT DEFAULT 0 COMMENT '销量',
  `rating`          DECIMAL(3,1) DEFAULT NULL COMMENT '评分',
  `category`        VARCHAR(100) DEFAULT NULL COMMENT '品类',
  `platform`        VARCHAR(50) DEFAULT NULL COMMENT '平台: 1688/taobao/jd/pdd',
  `image_url`       VARCHAR(1000) DEFAULT NULL COMMENT '商品图片URL',
  `detail_url`      VARCHAR(1000) DEFAULT NULL COMMENT '商品详情链接',
  `ai_score`        INT DEFAULT NULL COMMENT 'AI评分(0-100)',
  `verdict`         VARCHAR(20) DEFAULT NULL COMMENT 'AI决策: 上架/测试/放弃',
  `suggested_price`          DECIMAL(10,2) DEFAULT NULL COMMENT 'AI建议售价',
  `platform_suggest_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '平台建议零售价',
  `shipping_fee`             DECIMAL(10,2) DEFAULT NULL COMMENT '参考物流费',
  `delivery_in_48h`          TINYINT DEFAULT NULL COMMENT '是否支持48小时内发货(0否/1是)',
  `full_after_sales`         TINYINT DEFAULT NULL COMMENT '是否全包售后-退换货供应商承担(0否/1是)',
  `douyin_sheet_support`     TINYINT DEFAULT NULL COMMENT '是否支持抖音面单(0否/1是)',
  `auto_sync_stock`          TINYINT DEFAULT NULL COMMENT '是否支持自动同步库存(0否/1是)',
  `risk`            VARCHAR(200) DEFAULT NULL COMMENT 'AI风险提示',
  `ai_analysis`     TEXT DEFAULT NULL COMMENT 'AI分析文本',
  `ai_title`        VARCHAR(200) DEFAULT NULL COMMENT 'AI生成的吸引人新标题',
  `collect_date`    DATE DEFAULT NULL COMMENT '采集日期(去重用)',
  `collect_time`    DATETIME DEFAULT NULL COMMENT '采集时间',
  `collect_task_id` BIGINT DEFAULT NULL COMMENT '所属采集任务ID',
  `deleted`         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_num_iid_date` (`num_iid`, `collect_date`),
  INDEX `idx_platform_category` (`platform`, `category`),
  INDEX `idx_ai_score` (`ai_score` DESC),
  INDEX `idx_sales` (`sales` DESC),
  INDEX `idx_create_time` (`create_time` DESC),
  INDEX `idx_verdict` (`verdict`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================================
-- 采集任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS `collect_task` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_name`        VARCHAR(200) NOT NULL COMMENT '任务名称',
  `platform`         VARCHAR(50) NOT NULL COMMENT '平台',
  `keyword`          VARCHAR(200) NOT NULL COMMENT '关键词',
  `status`           VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
  `open_claw_task_id` VARCHAR(500) DEFAULT NULL COMMENT 'OpenClaw外部任务ID',
  `max_count`        INT DEFAULT 100 COMMENT '最大采集数量',
  `total_count`      INT DEFAULT 0 COMMENT '总商品数',
  `success_count`    INT DEFAULT 0 COMMENT '成功入库数',
  `error_msg`        TEXT DEFAULT NULL COMMENT '错误信息',
  `fill_detail_count`   INT DEFAULT NULL COMMENT '补全详情处理数量',
  `fill_detail_success` INT DEFAULT NULL COMMENT '补全详情成功数量',
  `fill_detail_failed`  INT DEFAULT NULL COMMENT '补全详情失败数量',
  `fill_detail_status`  VARCHAR(20) DEFAULT NULL COMMENT '补全详情状态: RUNNING/DONE/FAILED',
  `filter_drop_shipping` TINYINT DEFAULT NULL COMMENT '采集筛选:一件代发',
  `filter_delivery48h`   TINYINT DEFAULT NULL COMMENT '采集筛选:48小时发货',
  `filter_free_shipping` TINYINT DEFAULT NULL COMMENT '采集筛选:包邮',
  `filter_douyin_sheet`  TINYINT DEFAULT NULL COMMENT '采集筛选:抖音面单',
  `start_time`       DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time`         DATETIME DEFAULT NULL COMMENT '结束时间',
  `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务表';

-- ============================================================
-- 每日统计表
-- ============================================================
CREATE TABLE IF NOT EXISTS `daily_stats` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id`   BIGINT NOT NULL COMMENT '商品ID',
  `stat_date`    DATE NOT NULL COMMENT '统计日期',
  `sales`        BIGINT DEFAULT 0 COMMENT '当日销量',
  `views`        BIGINT DEFAULT 0 COMMENT '当日浏览量',
  `rating`       DECIMAL(3,1) DEFAULT NULL COMMENT '当日评分',
  `rank`         INT DEFAULT NULL COMMENT '当日排名',
  `price_change` DECIMAL(10,2) DEFAULT NULL COMMENT '价格变动',
  `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_date` (`product_id`, `stat_date`),
  INDEX `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- ============================================================
-- 关键词配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `keyword_config` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `keyword`     VARCHAR(200) NOT NULL COMMENT '关键词',
  `platform`    VARCHAR(50) NOT NULL DEFAULT '1688' COMMENT '平台',
  `category`    VARCHAR(100) DEFAULT NULL COMMENT '品类',
  `priority`    INT NOT NULL DEFAULT 5 COMMENT '优先级(1-10)',
  `enabled`     TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `cron_expr`   VARCHAR(100) DEFAULT '0 0 6 * * ?' COMMENT 'Cron表达式',
  `max_count`   INT NOT NULL DEFAULT 50 COMMENT '每次采集数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关键词配置表';

-- ============================================================
-- 排名历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ranking_history` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id`   BIGINT NOT NULL COMMENT '商品ID',
  `rank_type`    VARCHAR(20) NOT NULL COMMENT '排名类型: TODAY/WEEK/MONTH',
  `rank_position` INT NOT NULL COMMENT '排名位置',
  `score`        INT DEFAULT NULL COMMENT 'AI评分快照',
  `sales`        BIGINT DEFAULT NULL COMMENT '销量快照',
  `stat_date`    DATE NOT NULL COMMENT '统计日期',
  `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_product_type_date` (`product_id`, `rank_type`, `stat_date`),
  INDEX `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排名历史表';

-- ============================================================
-- 采集平台配置表（凭证存数据库，前端页面维护）
-- ============================================================
CREATE TABLE IF NOT EXISTS `platform_config` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`          VARCHAR(100) NOT NULL COMMENT '平台名称',
  `platform_type` VARCHAR(20)  NOT NULL COMMENT '平台类型: OPENCLAW / ALI_OPEN',
  `api_url`       VARCHAR(300) NOT NULL COMMENT 'API地址',
  `api_key`       VARCHAR(200) NOT NULL COMMENT 'API Key / App Key',
  `api_secret`    VARCHAR(200) DEFAULT NULL COMMENT 'API Secret / App Secret',
  `access_token`  VARCHAR(500) DEFAULT NULL COMMENT '1688开放平台 OAuth2 Access Token',
  `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '1688开放平台 OAuth2 Refresh Token',
  `is_active`     TINYINT NOT NULL DEFAULT 0 COMMENT '是否激活(同时只能一个)',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集平台配置表';

-- ============================================================
-- AI 分类分析报告表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_category_report` (
  `id`                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`                   VARCHAR(300) DEFAULT NULL COMMENT '报告标题',
  `platform`                VARCHAR(50)  DEFAULT NULL COMMENT '分析平台',
  `category`                VARCHAR(100) DEFAULT NULL COMMENT '分析品类',
  `hot_categories`          LONGTEXT     DEFAULT NULL COMMENT '当前热门分类 JSON',
  `rising_categories`       LONGTEXT     DEFAULT NULL COMMENT '即将热门分类 JSON',
  `product_recommendations` LONGTEXT     DEFAULT NULL COMMENT '分类选品推荐 JSON',
  `listing_advice`          LONGTEXT     DEFAULT NULL COMMENT '上架建议 JSON',
  `promotion_analysis`      LONGTEXT     DEFAULT NULL COMMENT '推广分析 JSON',
  `marketing_flow`          LONGTEXT     DEFAULT NULL COMMENT '营销推广流程 JSON',
  `ai_generated`            TINYINT      DEFAULT 1 COMMENT '是否AI生成(0=规则引擎,1=AI)',
  `create_time`             DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`             DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_create_time` (`create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分类分析报告表';

-- ============================================================
-- 初始化默认数据
-- ============================================================

-- 默认平台配置（1688开放平台）
INSERT IGNORE INTO `platform_config` (`id`, `name`, `platform_type`, `api_url`, `api_key`, `api_secret`, `is_active`, `remark`)
VALUES
  (1, 'OpenClaw (onebound)', 'OPENCLAW', 'https://api-gw.onebound.cn', 'your_openclaw_key', 'your_openclaw_secret', 0, '接口文档: https://open.onebound.cn'),
  (2, '1688开放平台',        'ALI_OPEN', 'https://gw.open.1688.com',   'your_1688_app_key', 'your_1688_app_secret', 1, '1688官方开放平台，需申请应用 App Key/Secret，接口: alibaba.product.search / alibaba.product.get');

-- 示例关键词配置
INSERT IGNORE INTO `keyword_config` (`keyword`, `platform`, `category`, `priority`, `enabled`, `max_count`)
VALUES
  ('篮球袜',   '1688', '运动鞋袜', 8, 1, 50),
  ('儿童玩具', '1688', '玩具',     7, 1, 50),
  ('手机壳',   '1688', '手机配件', 9, 1, 50);

-- ============================================================
-- 旧库升级存储过程（全新安装可忽略；已有旧库执行一次后自动删除）
-- 使用 CONTINUE HANDLER 忽略列已存在/不存在错误，可安全重复执行
-- ============================================================
DROP PROCEDURE IF EXISTS upgrade_schema;

DELIMITER $$
CREATE PROCEDURE upgrade_schema()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;  -- 忽略 Duplicate column name
    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END;  -- 忽略 Can't DROP
    DECLARE CONTINUE HANDLER FOR 1054 BEGIN END;  -- 忽略 Unknown column

    -- product 表补充字段
    ALTER TABLE `product` ADD COLUMN `platform_suggest_price` DECIMAL(10,2) DEFAULT NULL COMMENT '平台建议零售价';
    ALTER TABLE `product` ADD COLUMN `shipping_fee`           DECIMAL(10,2) DEFAULT NULL COMMENT '参考物流费';
    ALTER TABLE `product` CHANGE COLUMN `delivery_in48h` `delivery_in_48h` TINYINT DEFAULT NULL COMMENT '是否支持48小时内发货';
    ALTER TABLE `product` ADD COLUMN `delivery_in_48h`        TINYINT       DEFAULT NULL COMMENT '是否支持48小时内发货';
    ALTER TABLE `product` ADD COLUMN `full_after_sales`       TINYINT       DEFAULT NULL COMMENT '是否全包售后';
    ALTER TABLE `product` ADD COLUMN `douyin_sheet_support`   TINYINT       DEFAULT NULL COMMENT '是否支持抖音面单';
    ALTER TABLE `product` ADD COLUMN `auto_sync_stock`        TINYINT       DEFAULT NULL COMMENT '是否支持自动同步库存';
    ALTER TABLE `product` ADD COLUMN `ai_title`               VARCHAR(200)  DEFAULT NULL COMMENT 'AI生成的吸引人新标题';
    ALTER TABLE `product` ADD COLUMN `collect_task_id`        BIGINT        DEFAULT NULL COMMENT '所属采集任务ID';

    -- collect_task 表补充字段
    ALTER TABLE `collect_task` ADD COLUMN `fill_detail_count`    INT         DEFAULT NULL COMMENT '补全详情处理数量';
    ALTER TABLE `collect_task` ADD COLUMN `fill_detail_success`  INT         DEFAULT NULL COMMENT '补全详情成功数量';
    ALTER TABLE `collect_task` ADD COLUMN `fill_detail_failed`   INT         DEFAULT NULL COMMENT '补全详情失败数量';
    ALTER TABLE `collect_task` ADD COLUMN `fill_detail_status`   VARCHAR(20) DEFAULT NULL COMMENT '补全详情状态';
    ALTER TABLE `collect_task` ADD COLUMN `filter_drop_shipping` TINYINT     DEFAULT NULL COMMENT '采集筛选:一件代发';
    ALTER TABLE `collect_task` ADD COLUMN `filter_delivery48h`   TINYINT     DEFAULT NULL COMMENT '采集筛选:48小时发货';
    ALTER TABLE `collect_task` ADD COLUMN `filter_free_shipping` TINYINT     DEFAULT NULL COMMENT '采集筛选:包邮';
    ALTER TABLE `collect_task` ADD COLUMN `filter_douyin_sheet`  TINYINT     DEFAULT NULL COMMENT '采集筛选:抖音面单';

    -- platform_config 表补充字段
    ALTER TABLE `platform_config` ADD COLUMN `access_token`  VARCHAR(500) DEFAULT NULL COMMENT '1688 OAuth2 Access Token';
    ALTER TABLE `platform_config` ADD COLUMN `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '1688 OAuth2 Refresh Token';
END$$
DELIMITER ;

CALL upgrade_schema();
DROP PROCEDURE IF EXISTS upgrade_schema;
