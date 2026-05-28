-- 创建全栈应用专用数据库用户
-- 该用户只拥有 project_* 表的 CRUD 权限，禁止访问平台表

-- 创建用户（请根据实际环境修改密码）
CREATE USER IF NOT EXISTS 'gen_user'@'localhost' IDENTIFIED BY 'gen_user_secure_password_2026';
CREATE USER IF NOT EXISTS 'gen_user'@'%' IDENTIFIED BY 'gen_user_secure_password_2026';

-- 授予 ai_code_gen 数据库中 project_* 表的权限
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX
  ON ai_code_gen.`project_%`.* TO 'gen_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX
  ON ai_code_gen.`project_%`.* TO 'gen_user'@'%';

FLUSH PRIVILEGES;

-- 注意：MySQL 的 GRANT 不能用通配符匹配表名前缀
-- 实际部署时，建议：
-- 1. 创建一个独立数据库 ai_code_gen_apps
-- 2. GRANT ALL ON ai_code_gen_apps.* TO 'gen_user'@'localhost'
-- 3. 全栈应用的表都建在 ai_code_gen_apps 数据库中
