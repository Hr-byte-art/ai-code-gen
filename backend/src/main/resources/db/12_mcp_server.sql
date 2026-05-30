-- MCP 服务器配置表

CREATE TABLE IF NOT EXISTS `mcp_server` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '服务器名称',
  `url` VARCHAR(500) NOT NULL COMMENT '服务器URL',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `transport` VARCHAR(20) DEFAULT 'sse' COMMENT '传输类型: sse/stdio',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
  `tool_count` INT DEFAULT 0 COMMENT '工具数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP服务器配置表';
