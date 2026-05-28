-- 应用 Schema 记录表
-- 记录全栈应用创建的数据库表，用于删除应用时清理

CREATE TABLE IF NOT EXISTS `app_schema_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `app_id` BIGINT NOT NULL COMMENT '应用ID',
  `table_name` VARCHAR(200) NOT NULL COMMENT '表名（含前缀）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用Schema记录表';
