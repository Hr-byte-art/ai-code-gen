-- Skill 市场相关字段和表

-- code_skill 表新增市场字段
ALTER TABLE `code_skill` ADD COLUMN `is_public` TINYINT DEFAULT 0 COMMENT '是否公开到市场';
ALTER TABLE `code_skill` ADD COLUMN `use_count` INT DEFAULT 0 COMMENT '使用次数';
ALTER TABLE `code_skill` ADD COLUMN `author_id` BIGINT DEFAULT NULL COMMENT '作者用户ID';
ALTER TABLE `code_skill` ADD COLUMN `rating_avg` DOUBLE DEFAULT NULL COMMENT '平均评分(1-5)';
ALTER TABLE `code_skill` ADD COLUMN `rating_count` INT DEFAULT 0 COMMENT '评分人数';

-- Skill 安装记录表
CREATE TABLE IF NOT EXISTS `skill_install` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `skill_id` BIGINT NOT NULL COMMENT '技能ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `install_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_user` (`skill_id`, `user_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill安装记录表';

-- Skill 评分表
CREATE TABLE IF NOT EXISTS `skill_rating` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `skill_id` BIGINT NOT NULL COMMENT '技能ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `score` TINYINT NOT NULL COMMENT '评分(1-5)',
  `comment` VARCHAR(500) DEFAULT NULL COMMENT '评论',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_user` (`skill_id`, `user_id`),
  INDEX `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill评分表';
