-- 为 code_skill 表添加文件化 Skill 支持字段
-- content_hash: 内容哈希，用于变更检测
-- source: 数据来源，file=文件同步, manual=手动创建

ALTER TABLE `code_skill` ADD COLUMN `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容哈希（MD5）';
ALTER TABLE `code_skill` ADD COLUMN `source` VARCHAR(20) DEFAULT 'file' COMMENT '数据来源: file=文件, manual=手动';
