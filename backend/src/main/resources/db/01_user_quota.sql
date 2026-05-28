-- 用户配额配置表
-- 不同角色（普通用户/VIP/管理员）拥有不同的每日/每月生成次数和 Token 消耗额度

CREATE TABLE IF NOT EXISTS `user_quota` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_role` VARCHAR(20) NOT NULL COMMENT '角色: user/vip/admin',
  `daily_gen_limit` INT NOT NULL DEFAULT 10 COMMENT '每日生成次数上限',
  `monthly_gen_limit` INT NOT NULL DEFAULT 100 COMMENT '每月生成次数上限',
  `daily_token_limit` INT NOT NULL DEFAULT 500000 COMMENT '每日 Token 消耗上限',
  `monthly_token_limit` INT NOT NULL DEFAULT 5000000 COMMENT '每月 Token 消耗上限',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户配额配置表';

-- 初始配额数据
INSERT INTO `user_quota` (`id`, `user_role`, `daily_gen_limit`, `monthly_gen_limit`, `daily_token_limit`, `monthly_token_limit`) VALUES
(1, 'user', 10, 100, 500000, 5000000),
(2, 'vip', 50, 500, 2000000, 20000000),
(3, 'admin', 9999, 99999, 999999999, 999999999);
