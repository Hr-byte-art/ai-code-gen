-- 代码模板表
-- 存储内置代码模板，AI 基于模板做定制化修改

CREATE TABLE IF NOT EXISTS `code_template` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `template_key` VARCHAR(50) NOT NULL COMMENT '模板标识: login/admin/portfolio/landing/ecommerce',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
  `code_gen_type` VARCHAR(20) NOT NULL COMMENT '生成类型: html/multi_file/vue_project',
  `template_content` LONGTEXT NOT NULL COMMENT '模板代码内容(JSON格式存储多文件)',
  `preview_url` VARCHAR(500) DEFAULT NULL COMMENT '预览图URL',
  `use_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key` (`template_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码模板表';
