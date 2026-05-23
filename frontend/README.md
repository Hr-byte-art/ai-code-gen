# 熵擎 AI 应用生成平台

这是一个使用 Vue 3 + TypeScript + Vite 构建的 AI 应用生成平台前端项目。

## 功能特性

- 🏠 **首页** - 展示精选应用和快速创建入口
- 👤 **用户系统** - 登录、注册、个人中心
- 💬 **应用对话** - 与 AI 应用进行实时对话
- ✏️ **应用编辑** - 创建和编辑 AI 应用配置
- 📊 **Token 管理** - 查看 Token 使用情况和统计
- 🏆 **排行榜** - 用户和模型使用排行
- 🔧 **API 测试** - 在线测试 AI API 接口
- 👑 **管理后台** - 用户、应用、聊天管理

## 技术栈

- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **UI 组件库**: Ant Design Vue 4
- **HTTP 客户端**: Axios

## 项目结构

```
src/
├── api/              # API 接口
│   ├── index.ts      # 统一导出
│   ├── user.ts       # 用户相关
│   ├── app.ts        # 应用相关
│   ├── chat.ts       # 聊天相关
│   └── token.ts      # Token 相关
├── components/       # 公共组件
│   ├── Header.vue    # 头部导航
│   ├── Footer.vue    # 底部信息
│   └── AppCard.vue   # 应用卡片
├── layouts/          # 布局组件
│   └── BasicLayout.vue
├── router/           # 路由配置
│   └── index.ts
├── stores/           # 状态管理
│   ├── index.ts      # 统一导出
│   ├── user.ts       # 用户状态
│   ├── app.ts        # 应用状态
│   └── chat.ts       # 聊天状态
├── utils/            # 工具函数
│   └── request.ts    # HTTP 请求封装
├── views/            # 页面组件
│   ├── HomePage.vue
│   ├── user/         # 用户相关页面
│   ├── app/          # 应用相关页面
│   ├── token/        # Token 相关页面
│   └── admin/        # 管理后台页面
├── App.vue
└── main.ts
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 环境变量

创建 `.env` 文件配置环境变量：

```env
VITE_API_BASE_URL=/api
```

## 主要页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | 首页 | 展示精选应用 |
| `/user/login` | 登录页 | 用户登录 |
| `/user/register` | 注册页 | 用户注册 |
| `/user/center` | 个人中心 | 用户信息和设置 |
| `/app/chat/:id` | 应用对话 | 与 AI 应用对话 |
| `/app/edit/:id` | 应用编辑 | 编辑应用配置 |
| `/token` | Token 概览 | Token 使用统计 |
| `/token/details` | Token 详情 | 详细使用记录 |
| `/token/model` | 模型统计 | 各模型使用情况 |
| `/token/ranking` | 排行榜 | 用户和模型排行 |
| `/token/test` | API 测试 | 在线测试 API |
| `/admin/appManage` | 应用管理 | 管理员功能 |
| `/admin/chatManage` | 聊天管理 | 管理员功能 |
| `/admin/userManage` | 用户管理 | 管理员功能 |

## 开发说明

### 添加新页面

1. 在 `src/views/` 目录下创建页面组件
2. 在 `src/router/index.ts` 中添加路由配置
3. 如需 API，在 `src/api/` 目录下添加接口
4. 如需状态管理，在 `src/stores/` 目录下添加 store

### 代码规范

- 使用 TypeScript 进行类型检查
- 使用 Composition API (`<script setup>`)
- 组件使用 PascalCase 命名
- 文件名使用 camelCase 或 PascalCase

## 许可证

MIT
