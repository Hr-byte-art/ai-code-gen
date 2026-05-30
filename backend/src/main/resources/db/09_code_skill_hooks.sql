-- 为 code_skill 表添加生命周期钩子字段
-- hooks: JSON 对象，定义 beforeGenerate/afterGenerate 钩子

ALTER TABLE `code_skill` ADD COLUMN `hooks` JSON DEFAULT NULL COMMENT '生命周期钩子定义（JSON对象）';
