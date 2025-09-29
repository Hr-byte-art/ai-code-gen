# 熵擎 AI 代码生成平台 - 后端服务

<div align="center">
  <h1> 熵擎——从想法到应用的桥梁</h1>
  <p>基于AI技术的智能代码生成平台后端服务</p>
  
  ![Java](https://img.shields.io/badge/Java-17-orange)
  ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-green)
  ![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue)
  ![Redis](https://img.shields.io/badge/Redis-6.0+-red)
  ![License](https://img.shields.io/badge/License-MIT-yellow)
</div>

## 📖 项目简介

熵擎是一个基于AI技术的智能代码生成平台，能够根据用户的自然语言描述自动生成完整的Web应用程序。本项目为后端服务，提供核心的AI代码生成、用户管理、应用部署等功能。

### ✨ 核心特性

- 🤖 **多模型AI驱动**：
  - DeepSeek Chat：主要代码生成模型
  - DeepSeek Reasoner：复杂推理任务
  - 通义千问：智能路由和内容检测
  - 万相生图：AI图片生成

- **多种应用类型**：
  - HTML静态页面
  - Vue.js单页应用
  - 多文件复杂项目
  - 响应式Web应用
  
- **实时流式生成**：
  - SSE（Server-Sent Events）技术
  - 实时显示生成过程
  - 支持生成过程中断和恢复

- **一键智能部署**：
  - 自动构建项目
  - 部署到指定服务器
  - 生成访问链接和截图
  - 支持多环境部署配置
  
- 👥 **完整用户体系**：
  - 用户注册/登录/权限管理
  - VIP会员体系和兑换码系统
  - 用户行为统计和分析

- **数据管理**：
  - 应用生成历史记录
  - 对话历史保存
  - 文件上传和存储管理
  - 腾讯云COS集成
  
- 🛡️ **安全可靠**：
  - JWT身份认证
  - 接口权限控制
  - 限流防护
  - 输入内容安全检测

## 🏗️ 技术架构

### 核心技术栈
- **框架**：Spring Boot 3.0+
- **数据库**：MySQL 8.0+
- **缓存**：Redis 6.0+
- **AI集成**：LangChain4j
- **文档**：Knife4j (Swagger)
- **构建工具**：Maven
- **Java版本**：JDK 17

### AI模型支持
- **DeepSeek Chat**：主要代码生成模型
- **DeepSeek Reasoner**：复杂推理任务
- **通义千问**：智能路由和检测
- **万相生图**：图片生成服务

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 1. 克隆项目
```bash
git clone https://github.com/Hr-byte-art/ai-code-gen.git
cd ai-code-gen
```

### 2. 数据库配置
```sql
-- 创建数据库
CREATE DATABASE ai_code_gen CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入数据库结构（如果有SQL文件）
-- mysql -u root -p ai_code_gen < database.sql
```

### 3. 配置文件
创建并修改配置文件 `src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ai_code_gen?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false
    username: your_db_username
    password: your_db_password
  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      ttl: 3600
      password: your_redis_password  # 如果Redis没有密码可留空

# AI模型配置
langchain4j:
  open-ai:
    # 主要AI模型配置（DeepSeek）
    chat-model:
      base-url: https://api.deepseek.com
      api-key: your_deepseek_api_key
      model-name: deepseek-chat
      log-requests: true
      log-responses: true
      max-tokens: 8192
    # 流式对话模型
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: your_deepseek_api_key
      model-name: deepseek-chat
      max-tokens: 8192
    # 推理模型配置（复杂推理任务）
    reasoning-streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: your_deepseek_api_key
      model-name: deepseek-reasoner
      max-tokens: 32768
      temperature: 0.1
    # 智能路由模型（通义千问）
    routing-chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: your_qwen_api_key
      model-name: qwen-turbo
    # 智能检测模型
    detection-chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: your_qwen_api_key
      model-name: qwen-turbo
    # 应用名称生成模型
    generation-appName-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: your_qwen_api_key
      model-name: qwen-turbo

# 腾讯云COS对象存储配置
cos:
  client:
    host: https://your-bucket.cos.your-region.myqcloud.com
    secretId: your_cos_secret_id
    secretKey: your_cos_secret_key
    region: your_cos_region
    bucket: your_cos_bucket

# 图片搜索API配置
picture:
  search:
    api-key: your_picture_search_api_key

# 阿里云DashScope配置（万相生图）
dashscope:
  api-key: your_dashscope_api_key
  image-model: wan2.2-t2i-flash
```

> **配置说明**：
> - **DeepSeek API**: 用于主要的代码生成任务，需要从 [DeepSeek官网](https://api.deepseek.com) 获取
> - **通义千问API**: 用于智能路由和检测，需要从 [阿里云百炼](https://bailian.console.aliyun.com) 获取
> - **腾讯云COS**: 用于文件存储，需要从腾讯云控制台获取
> - **万相生图API**: 用于图片生成，需要从阿里云DashScope获取

### 4. 启动应用
```bash
# 开发环境
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/ai-code-gen-0.0.1-SNAPSHOT.jar
```

### 5. 访问应用
- **API文档**：http://localhost:8123/api/doc.html
- **健康检查**：http://localhost:8123/api/actuator/health

## 📁 项目结构

```
ai-code-gen/
├── src/main/java/com/wjh/aicodegen/
│   ├── ai/                     # AI服务模块
│   │   ├── factory/           # AI服务工厂
│   │   └── service/           # AI服务实现
│   ├── config/                # 配置类
│   ├── controller/            # 控制器
│   ├── core/                  # 核心功能
│   │   ├── deploy/           # 部署服务
│   │   ├── handler/          # 流处理器
│   │   └── builder/          # 构建器
│   ├── model/                 # 数据模型
│   │   ├── entity/           # 实体类
│   │   ├── dto/              # 数据传输对象
│   │   └── vo/               # 视图对象
│   ├── service/              # 业务服务
│   └── utils/                # 工具类
├── src/main/resources/
│   ├── mapper/               # MyBatis映射文件
│   ├── prompt/               # AI提示词模板
│   ├── application.yml       # 主配置文件
│   ├── application-local.yml # 本地环境配置
│   └── application-prod.yml  # 生产环境配置
└── tmp/                      # 临时文件目录
    ├── code_output/          # 代码生成输出
    └── code_deploy/          # 代码部署目录
```

## 🔧 配置说明

### 环境配置
项目支持多环境配置：
- `application.yml`：基础配置
- `application-local.yml`：本地开发环境
- `application-prod.yml`：生产环境

### 核心配置项
```yaml
# 代码生成配置
code:
  deploy-host: http://your-domain.com
  deploy-port: 80
  deploy-path: dist

# 对象存储配置（腾讯云COS）
cos:
  client:
    host: https://your-bucket.cos.region.myqcloud.com
    secretId: your_secret_id
    secretKey: your_secret_key
    region: your_region
    bucket: your_bucket
```

## 📚 API文档

启动应用后，访问 http://localhost:8123/api/doc.html 查看完整的API文档。

### 主要接口模块

#### 📱 应用管理 (`AppController`)
- **路径前缀**：`/api/app`
- **主要功能**：
  - `POST /chat/generate` - AI代码生成（支持流式响应）
  - `POST /deploy` - 应用一键部署
  - `POST /screenshot` - 应用截图生成
  - `GET /list/page` - 分页查询应用列表
  - `POST /add` - 创建新应用
  - `POST /update` - 更新应用信息
  - `POST /delete` - 删除应用
  - `GET /my` - 获取我的应用列表
  - `POST /upload/file` - 文件上传

#### 👤 用户管理 (`UserController`)
- **路径前缀**：`/api/user`
- **主要功能**：
  - `POST /register` - 用户注册
  - `POST /login` - 用户登录
  - `POST /logout` - 用户登出
  - `GET /get/login` - 获取当前登录用户
  - `GET /list/page` - 分页查询用户（管理员）
  - `POST /update` - 更新用户信息
  - `POST /delete` - 删除用户（管理员）

#### 💎 VIP会员管理 (`VipController`, `VipCodeController`)
- **路径前缀**：`/api/vip`
- **主要功能**：
  - `POST /vipCodeRedemption` - VIP码兑换
  - `POST /vipCode/add` - 添加VIP码（管理员）
  - `GET /vipCode/list/page` - VIP码管理
  - `POST /vipCode/delete` - 删除VIP码

#### 💬 对话历史 (`ChatHistoryController`)
- **路径前缀**：`/api/chatHistory`
- **主要功能**：
  - `GET /list/page` - 分页查询对话历史
  - `POST /delete` - 删除对话记录

#### 🏥 系统监控 (`HealthController`)
- **路径前缀**：`/api/health`
- **主要功能**：
  - `GET /check` - 系统健康检查
  - 监控应用运行状态

#### 📁 静态资源 (`StaticResourceController`)
- **主要功能**：
  - 处理静态文件访问
  - 支持生成应用的预览和下载

## 部署指南

### Docker部署（推荐）
```dockerfile
# Dockerfile示例
FROM openjdk:17-jdk-slim
COPY target/ai-code-gen-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8123
ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"]
```

### 传统部署
```bash
# 打包应用
mvn clean package -DskipTests

# 上传到服务器并启动
nohup java -jar ai-code-gen-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
```

### Nginx配置
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location /api/ {
        proxy_pass http://127.0.0.1:8123;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 🔍 监控与日志

### 应用监控
- **健康检查**：`/api/actuator/health`
- **应用信息**：`/api/actuator/info`
- **指标监控**：`/api/actuator/prometheus`

### 日志配置
```yaml
logging:
  level:
    com.wjh.aicodegen: DEBUG
    org.springframework.web: INFO
  file:
    name: logs/ai-code-gen.log
```

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

## 📄 开源协议

本项目采用 MIT 协议 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👨‍💻 作者

**王锦辉**
- 邮箱：wanghaha1217@126.com
- 网站：entropyqing.asia

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的Java应用框架
- [LangChain4j](https://github.com/langchain4j/langchain4j) - Java版本的LangChain
- [DeepSeek](https://www.deepseek.com/) - 优秀的AI模型服务
- [Ant Design](https://ant.design/) - 企业级UI设计语言

## 📈 更新日志

### v1.0.0 (2024-12-01)
- ✨ 初始版本发布
- 🤖 集成AI代码生成功能
- 👥 用户管理系统
- 应用部署功能
- VIP会员体系

---

<div align="center">
  <p>如果这个项目对你有帮助，请给一个 ⭐ Star！</p>
  <p>© 2024 版权归 王哈哈 所有 | 本站应用内容由AI技术生成</p>
</div>
