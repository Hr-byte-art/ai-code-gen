-- 扩大对话历史消息字段
-- AI 生成结果、工具执行摘要和错误堆栈可能超过 VARCHAR/TEXT 的旧长度限制

ALTER TABLE `chat_history`
    MODIFY COLUMN `message` LONGTEXT NOT NULL COMMENT '消息';