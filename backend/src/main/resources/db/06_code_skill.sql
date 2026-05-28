-- 代码生成技能表
-- 将硬编码的 CodeGenTypeEnum + prompt + 工具配置抽象为可配置的 Skill

CREATE TABLE IF NOT EXISTS `code_skill` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '技能名称',
  `skill_key` VARCHAR(50) NOT NULL COMMENT '唯一标识',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '技能描述',
  `system_prompt` TEXT NOT NULL COMMENT '系统提示词',
  `code_gen_type` VARCHAR(30) NOT NULL COMMENT '代码生成类型',
  `point_cost` INT NOT NULL DEFAULT 10 COMMENT '积分消耗',
  `tool_names` VARCHAR(500) DEFAULT NULL COMMENT '可用工具列表，逗号分隔，null表示全部',
  `build_strategy` VARCHAR(30) NOT NULL DEFAULT 'none' COMMENT '构建策略: none/vue/fullstack',
  `model_strategy` VARCHAR(30) NOT NULL DEFAULT 'standard' COMMENT '模型策略: standard/reasoning',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_key` (`skill_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成技能表';

-- 初始数据将在应用启动时通过 CodeSkillDataInitializer 从 prompt 文件读取并插入
