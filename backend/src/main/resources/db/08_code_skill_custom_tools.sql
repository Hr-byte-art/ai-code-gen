-- 为 code_skill 表添加自定义工具字段
-- custom_tools: JSON 数组，定义 Skill 自带的 HTTP 工具

ALTER TABLE `code_skill` ADD COLUMN `custom_tools` JSON DEFAULT NULL COMMENT '自定义工具定义（JSON数组）';
