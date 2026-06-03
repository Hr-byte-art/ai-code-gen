-- 应用表增加设计风格标识

ALTER TABLE `app`
  ADD COLUMN `designKey` VARCHAR(100) DEFAULT NULL COMMENT '设计风格标识' AFTER `codeGenType`;