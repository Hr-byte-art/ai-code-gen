-- 给 token_usage_record 表增加 update_time 和 version 列
-- update_time: 记录最后更新时间，保留原始 create_time 不变
-- version: 乐观锁版本号，解决并发更新丢失 Token 问题

ALTER TABLE `token_usage_record`
  ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '最后更新时间' AFTER `createTime`,
  ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `update_time`;

-- 将已有记录的 update_time 初始化为 createTime
UPDATE `token_usage_record` SET `update_time` = `createTime` WHERE `update_time` IS NULL;
