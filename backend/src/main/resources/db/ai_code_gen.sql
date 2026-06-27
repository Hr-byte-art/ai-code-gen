/*
 Navicat Premium Dump SQL

 Source Server         : localhost_mysql
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : ai_code_gen

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 15/06/2026 11:08:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_trace
-- ----------------------------
DROP TABLE IF EXISTS `agent_trace`;
CREATE TABLE `agent_trace`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '追踪ID',
  `agent_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Agent名称',
  `app_id` bigint NULL DEFAULT NULL COMMENT '关联应用ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '关联用户ID',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型名称',
  `input_tokens` int NULL DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` int NULL DEFAULT 0 COMMENT '输出Token数',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'success' COMMENT '执行状态',
  `review_result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审查结果',
  `review_score` int NULL DEFAULT NULL COMMENT '审查评分',
  `issues` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '问题列表',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '执行耗时',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE,
  INDEX `idx_app_id`(`app_id` ASC) USING BTREE,
  INDEX `idx_agent_name`(`agent_name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent执行追踪表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用封面',
  `initPrompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '应用初始化的 prompt',
  `codeGenType` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '代码生成类型（枚举）',
  `designKey` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设计风格标识',
  `deployKey` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '部署标识',
  `deployedTime` datetime NULL DEFAULT NULL COMMENT '部署时间',
  `buildStatus` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none' COMMENT '构建状态：none/pending/building/success/failed',
  `buildMessage` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '构建状态说明',
  `buildError` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '构建错误摘要',
  `buildRetryCount` int NOT NULL DEFAULT 0 COMMENT '构建重试次数',
  `buildStartedTime` datetime NULL DEFAULT NULL COMMENT '构建开始时间',
  `buildFinishedTime` datetime NULL DEFAULT NULL COMMENT '构建完成时间',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_deployKey`(`deployKey` ASC) USING BTREE,
  INDEX `idx_appName`(`appName` ASC) USING BTREE,
  INDEX `idx_userId`(`userId` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 420011132187783169 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_schema_record
-- ----------------------------
DROP TABLE IF EXISTS `app_schema_record`;
CREATE TABLE `app_schema_record`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表名（含前缀）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_id`(`app_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用Schema记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
  `messageType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
  `appId` bigint NOT NULL COMMENT '应用id',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appId`(`appId` ASC) USING BTREE,
  INDEX `idx_createTime`(`createTime` ASC) USING BTREE,
  INDEX `idx_appId_createTime`(`appId` ASC, `createTime` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 420013562124251137 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '对话历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_skill
-- ----------------------------
DROP TABLE IF EXISTS `code_skill`;
CREATE TABLE `code_skill`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '技能名称',
  `skill_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '唯一标识',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '技能描述',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统提示词',
  `code_gen_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '代码生成类型',
  `point_cost` int NOT NULL DEFAULT 10 COMMENT '积分消耗',
  `tool_names` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '可用工具列表，逗号分隔，null表示全部',
  `build_strategy` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none' COMMENT '构建策略: none/vue/fullstack',
  `model_strategy` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'standard' COMMENT '模型策略: standard/reasoning',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容哈希',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'db' COMMENT '来源: file/db',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  `custom_tools` json NULL COMMENT '自定义工具定义（JSON数组）',
  `hooks` json NULL COMMENT '生命周期钩子定义（JSON对象）',
  `is_public` tinyint NULL DEFAULT 0 COMMENT '是否公开到市场',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `author_id` bigint NULL DEFAULT NULL COMMENT '作者用户ID',
  `rating_avg` double NULL DEFAULT NULL COMMENT '平均评分(1-5)',
  `rating_count` int NULL DEFAULT 0 COMMENT '评分人数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_key`(`skill_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '代码生成技能表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_template
-- ----------------------------
DROP TABLE IF EXISTS `code_template`;
CREATE TABLE `code_template`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `template_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板标识: login/admin/portfolio/landing/ecommerce',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板描述',
  `code_gen_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '生成类型: html/multi_file/vue_project',
  `template_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板代码内容(JSON格式存储多文件)',
  `preview_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预览图URL',
  `use_count` int NOT NULL DEFAULT 0 COMMENT '使用次数',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_key`(`template_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '代码模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mcp_server
-- ----------------------------
DROP TABLE IF EXISTS `mcp_server`;
CREATE TABLE `mcp_server`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务器名称',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '服务器URL',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `transport` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'sse' COMMENT '传输类型',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `tool_count` int NULL DEFAULT 0 COMMENT '工具数量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP服务器配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for skill_install
-- ----------------------------
DROP TABLE IF EXISTS `skill_install`;
CREATE TABLE `skill_install`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `skill_id` bigint NOT NULL COMMENT '技能ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `install_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_user`(`skill_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Skill安装记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for skill_rating
-- ----------------------------
DROP TABLE IF EXISTS `skill_rating`;
CREATE TABLE `skill_rating`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `skill_id` bigint NOT NULL COMMENT '技能ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `score` tinyint NOT NULL COMMENT '评分(1-5)',
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '评论',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_user`(`skill_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_skill_id`(`skill_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Skill评分表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for token_usage_record
-- ----------------------------
DROP TABLE IF EXISTS `token_usage_record`;
CREATE TABLE `token_usage_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID - 使用雪花ID生成器',
  `userId` bigint NULL DEFAULT NULL COMMENT '用户ID，关联user表',
  `appId` bigint NULL DEFAULT NULL COMMENT '应用ID，关联app表',
  `modelName` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI模型名称：deepseek-chat, qwen-turbo等',
  `aiCallPurpose` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI调用用途：ROUTING, CODE_GENERATION, IMAGE_SEARCH等',
  `inputTokens` int NOT NULL DEFAULT 0 COMMENT '输入Token数量（对应实体类requestTokenCount）',
  `outputTokens` int NOT NULL DEFAULT 0 COMMENT '输出Token数量（对应实体类responseTokenCount）',
  `totalTokens` int NOT NULL DEFAULT 0 COMMENT '总Token数量',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updateTime` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_app`(`userId` ASC, `appId` ASC) USING BTREE,
  INDEX `idx_create_time`(`createTime` ASC) USING BTREE,
  INDEX `idx_model_name`(`modelName` ASC) USING BTREE,
  INDEX `idx_ai_call_purpose`(`aiCallPurpose` ASC) USING BTREE,
  INDEX `idx_purpose_time`(`aiCallPurpose` ASC, `createTime` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 420013561994227713 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Token使用记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `vipExpireTime` datetime NULL DEFAULT NULL COMMENT '会员过期时间',
  `vipCode` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会员兑换码',
  `vipNumber` bigint NULL DEFAULT NULL COMMENT '会员编号',
  `shareCode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分享码',
  `inviteUser` bigint NULL DEFAULT NULL COMMENT '邀请用户 id',
  `integral` int NULL DEFAULT NULL COMMENT '用户积分',
  `recentlySignedIn` datetime NULL DEFAULT NULL COMMENT '最近签到时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_userAccount`(`userAccount` ASC) USING BTREE,
  INDEX `idx_userName`(`userName` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 415786062179364865 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_quota
-- ----------------------------
DROP TABLE IF EXISTS `user_quota`;
CREATE TABLE `user_quota`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色: user/vip/admin',
  `daily_gen_limit` int NOT NULL DEFAULT 10 COMMENT '每日生成次数上限',
  `monthly_gen_limit` int NOT NULL DEFAULT 100 COMMENT '每月生成次数上限',
  `daily_token_limit` int NOT NULL DEFAULT 500000 COMMENT '每日 Token 消耗上限',
  `monthly_token_limit` int NOT NULL DEFAULT 5000000 COMMENT '每月 Token 消耗上限',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_role` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户配额配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for vip_code
-- ----------------------------
DROP TABLE IF EXISTS `vip_code`;
CREATE TABLE `vip_code`  (
  `Id` bigint NOT NULL COMMENT '主键',
  `vipCode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'vipCode',
  `effectiveDay` bigint NOT NULL COMMENT 'vipCode的兑换的VIP的有效时间（天）',
  `expDate` datetime NOT NULL COMMENT '该code的过期时间',
  `idDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `useNum` int NOT NULL DEFAULT 0 COMMENT '使用人数',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `createUser` bigint NOT NULL COMMENT '创建人',
  `maxUseNum` int NULL DEFAULT NULL COMMENT '最大使用人数',
  PRIMARY KEY (`Id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
