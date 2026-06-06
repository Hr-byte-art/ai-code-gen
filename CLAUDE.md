# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

熵擎 AI 代码生成平台 — 用户通过自然语言描述自动生成 Web 应用（HTML / 多文件 / Vue 工程），支持流式输出、一键部署、VIP 会员体系。

## 常用命令

### 后端 (backend/)

```bash
cd backend
mvn spring-boot:run                    # 启动开发服务 (localhost:8123/api)
mvn clean package -DskipTests          # 打包
mvn test -Dtest=ClassName#methodName   # 运行单个测试
java -jar target/ai-code-gen-0.0.1-SNAPSHOT.jar  # 运行 jar
```

API 文档：`http://localhost:8123/api/doc.html` (Knife4j)

### 前端 (frontend/)

```bash
cd frontend
npm install       # 安装依赖
npm run dev       # 启动开发服务 (localhost:5173, 代理 /api → localhost:8123)
npm run build     # vue-tsc 类型检查 + vite 构建
npm run preview   # 预览生产构建
```

## 技术栈

### 后端

- **Java 21** + Spring Boot 3.5.4
- **ORM**: MyBatis-Flex（不是 MyBatis-Plus）
- **缓存**: Redis (Spring Session + Redisson) + Caffeine 本地缓存
- **AI**: LangChain4j 1.1.0 + LangGraph4j 1.6.0
- **对象映射**: MapStruct 1.6.3 + Lombok（已配置 `unmappedTargetPolicy = IGNORE`）
- **API 文档**: Knife4j 4.4.0 (springdoc-openapi3)
- **对象存储**: 腾讯云 COS
- **监控**: Spring Actuator + Micrometer Prometheus

### 前端

- **Vue 3** + TypeScript + Composition API (`<script setup>`)
- **构建**: Vite 8
- **路由**: Vue Router 4
- **状态管理**: Pinia 3
- **UI**: Ant Design Vue 4
- **HTTP**: Axios（`/api` 代理到后端 8123 端口）

## 架构要点

### 代码生成流程

核心入口：`AiCodeGeneratorFacade.generateAndSaveCodeStream()`

三种生成类型（`CodeGenTypeEnum`）：
- `HTML` — 单 HTML 文件
- `MULTI_FILE` — 多文件项目
- `VUE_PROJECT` — Vue 工程（需构建步骤，使用 `VueProjectBuilder`）

流程：AI 生成代码 → `CodeParserExecutor` 解析 → `CodeFileSaverExecutor` 保存到 `tmp/code_output/`

### LangGraph4j 工作流

`langgraph4j` 包实现了基于图的代码生成工作流：
- 节点：`ImageCollectorNode` → `PromptEnhancerNode` → `RouterNode` → `CodeGeneratorNode` → `CodeQualityCheckNode` → `ProjectBuilderNode`
- 质检失败会重新生成（最多 5 次）
- `WorkflowContext` 通过 `state` 在节点间传递上下文

### AI 服务工厂模式

`ai/factory/` 下的工厂类为每个应用创建独立的 AI 服务实例：
- `AiCodeGeneratorServiceFactory` — 代码生成
- `AiCodeGenTypeRoutingServiceFactory` — 路由决策
- `AiInputPromptDetectionServiceFactory` — 输入安全检测
- `AiGenerateAppNameServiceFactory` — 应用名生成

### 分层模型

- `model/entity/` — 数据库实体（MyBatis-Flex `@Table` 注解）
- `model/dto/` — 请求 DTO
- `model/vo/` — 响应 VO
- `convert/` — MapStruct 转换器

### 前端架构

- `api/` — 按模块拆分的 API 接口（user, app, chat, token），统一通过 `utils/request.ts` 的 Axios 实例调用
- `stores/` — Pinia store（user, app, chat），Composition API 风格
- `types/api.ts` — 全部 TypeScript 接口定义
- `views/` — 按功能域分目录：user/, app/, token/, admin/
- `router/index.ts` — 路由守卫实现权限控制（`requiresAuth` / `requiresAdmin` meta）
- `layouts/BasicLayout.vue` — Header + router-view + Footer 的基础布局

### 权限控制

- 后端：`@AuthCheck` 自定义注解 + `AuthInterceptor` AOP 切面，Session 存 Redis（30 天有效期）
- 前端：路由守卫检查 `meta.requiresAuth` / `meta.requiresAdmin`，未登录跳转 `/user/login`

## 配置文件

- `backend/application.yml` — 主配置
- `backend/application-local.yml` — 本地开发（`.gitignore` 已忽略）
- `backend/application-prod.yml` — 生产环境（`.gitignore` 已忽略）
- `backend/src/main/resources/prompt/` — AI 提示词模板文件
- `frontend/vite.config.ts` — Vite 配置，`@` alias 指向 `src/`，开发代理 `/api` → `localhost:8123`

## 注意事项

- 代码生成输出目录 `tmp/` 已在 `.gitignore` 中
- `application-local.yml` 和 `application-prod.yml` 包含敏感信息，不要提交
- 前端构建需先通过 `vue-tsc` 类型检查，类型错误会阻止构建
- 后端使用 MyBatis-Flex 而非 MyBatis-Plus，API 和注解有区别
