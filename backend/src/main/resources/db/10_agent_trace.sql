-- Agent 执行追踪表
-- 记录每次 Agent 调用的耗时、Token 消耗、状态等信息

CREATE TABLE IF NOT EXISTS `agent_trace` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `trace_id` VARCHAR(64) NOT NULL COMMENT '追踪ID（同一次生成流程共享）',
  `agent_name` VARCHAR(50) NOT NULL COMMENT 'Agent名称: review/optimizer/generator/router',
  `app_id` BIGINT DEFAULT NULL COMMENT '关联应用ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '关联用户ID',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '使用的模型名称',
  `input_tokens` INT DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` INT DEFAULT 0 COMMENT '输出Token数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '执行状态: success/error/timeout',
  `review_result` VARCHAR(20) DEFAULT NULL COMMENT '审查结果: passed/failed/skipped',
  `review_score` INT DEFAULT NULL COMMENT '审查评分(0-100)',
  `issues` TEXT DEFAULT NULL COMMENT '问题列表(JSON)',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_trace_id` (`trace_id`),
  INDEX `idx_app_id` (`app_id`),
  INDEX `idx_agent_name` (`agent_name`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent执行追踪表';
