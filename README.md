# 熵擎 AI 代码生成平台

熵擎是一个 AI 代码生成平台，支持通过自然语言生成 Web 应用，并提供流式输出、一键部署、应用管理、会员与用量统计等能力。

## 核心能力

- 支持 HTML、多文件项目、Vue 工程和全栈项目生成
- 支持 SSE 流式生成反馈
- 支持应用一键部署和预览
- 支持全栈应用通过统一后端入口访问
- 支持用户登录、权限控制、VIP 会员和配额管理
- 支持 AI 调用监控、Token 统计和后台管理

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.4
- MyBatis-Flex
- Redis / Spring Session / Redisson / Caffeine
- LangChain4j / LangGraph4j
- Knife4j
- 腾讯云 COS

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Ant Design Vue
- Axios

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认访问：

```text
http://localhost:8123/api
```

接口文档：

```text
http://localhost:8123/api/doc.html
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

开发环境前端会把 `/api` 代理到后端 `8123` 端口。

## 打包部署

### 后端打包

```bash
cd backend
mvn clean package -DskipTests
```

运行 Jar：

```bash
java -jar target/ai-code-gen-0.0.1-SNAPSHOT.jar
```

### 前端打包

```bash
cd frontend
npm run build
```

构建产物位于：

```text
frontend/dist
```

## 全栈应用部署说明

全栈生成项目不会直接把 Express 端口暴露给浏览器。

部署后统一通过主后端代理入口访问：

```text
/api/fullstack/{appId}/
```

示例：

```text
http://your-domain/api/fullstack/420419301581987840/
```

内部流程：

```text
浏览器
  -> Nginx
  -> Spring Boot 主后端
  -> /api/fullstack/{appId}/
  -> 本机对应 Express 服务
```

因此生产环境通常不需要额外开放 `3001`、`3002` 这类 Node 端口。

## 配置说明

敏感配置不要提交到仓库。

常见本地 / 生产配置：

```text
backend/src/main/resources/application-local.yml
backend/src/main/resources/application-prod.yml
```

这些文件应通过服务器配置、环境变量或本地私有文件维护。

## 注意事项

- 前端构建前会执行 TypeScript 检查，类型错误会阻止构建
- 后端 ORM 使用 MyBatis-Flex，不是 MyBatis-Plus
- 代码生成输出目录 `tmp/` 不应提交
- AI API Key、数据库密码、云服务密钥不要提交到仓库