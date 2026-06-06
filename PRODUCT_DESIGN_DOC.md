# 熵擎 AI 代码生成平台 — 产品级架构设计文档

> 版本: v1.0 | 日期: 2026-05-29 | 作者: AI Assistant

---

## 一、当前系统分析

### 1.1 已有能力

| 模块 | 状态 | 说明 |
|------|:----:|------|
| 代码生成 | ✅ | 支持 9 种类型（HTML/多文件/Vue/React/Next.js/全栈/着陆页/管理后台/Node.js API） |
| 工具调用 | ✅ | 22 个工具（文件操作/图片搜索/词典/二维码/配色等） |
| 流式输出 | ✅ | SSE 流式传输，支持断线重连 |
| 部署预览 | ✅ | 静态资源服务 + 一键部署 |
| 用户系统 | ✅ | 注册/登录/VIP/积分/签到/邀请 |
| 监控统计 | ✅ | Token 消耗/模型统计/Prometheus 指标 |
| 权限控制 | ✅ | 角色鉴权 + 限流 |
| 多 Agent | ⚠️ | 已实现 ReviewAgent + CodeOptimizerAgent，但未接入主流程 |

### 1.2 核心问题

| # | 问题 | 严重程度 | 影响 |
|---|------|:--------:|------|
| 1 | **Skill 只是配置，不是真正的可执行单元** | 高 | 无法运行脚本、调用外部服务、执行自定义逻辑 |
| 2 | **LangGraph4j 工作流未接入主流程** | 高 | 已有完整基础设施但没用，主流程是手写的 |
| 3 | **多 Agent 只在 processCodeStream 生效** | 中 | 工具增强模式（Vue/React/全栈）不走审查 |
| 4 | **Skill 无法扩展工具** | 中 | 只能用预定义的 22 个工具，无法动态注册 |
| 5 | **部署与预览耦合** | 中 | 必须点击部署才能预览（已部分修复） |
| 6 | **Token 统计跨线程丢失** | 中 | MonitorContext 在多线程环境下不稳定 |
| 7 | **无 Skill 市场和分享机制** | 低 | 用户无法共享和安装第三方 Skill |
| 8 | **前端部分页面无深色模式适配** | 低 | 用户消息在深色模式下不可见（已修复） |

---

## 二、目标架构

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 首页     │  │ 工作台   │  │ 管理后台 │  │ Skill 市场│        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└───────────────────────────┬─────────────────────────────────────┘
                            │ SSE + REST API
┌───────────────────────────┴─────────────────────────────────────┐
│                     API Gateway (Spring Boot)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 认证     │  │ 限流     │  │ 路由     │  │ 监控     │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────────┐
│                    Agent 编排层 (LangGraph4j)                    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   Orchestrator Graph                     │    │
│  │                                                          │    │
│  │  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐ │    │
│  │  │ Router  │──▶│Generator│──▶│ Reviewer│──▶│ Optimizer│ │    │
│  │  │ Agent   │   │ Agent   │   │ Agent   │   │ Agent    │ │    │
│  │  └─────────┘   └─────────┘   └─────────┘   └─────────┘ │    │
│  │       │              │             │             │       │    │
│  │       ▼              ▼             ▼             ▼       │    │
│  │  ┌─────────────────────────────────────────────────┐    │    │
│  │  │              Tool Registry                       │    │    │
│  │  │  内置工具 + Skill 自定义工具 + MCP 工具           │    │    │
│  │  └─────────────────────────────────────────────────┘    │    │
│  └──────────────────────────────────────────────────────────┘    │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────────┐
│                       Skill 引擎层                               │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Skill Runtime                            │   │
│  │                                                           │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │   │
│  │  │ Prompt  │  │ Tool    │  │ Script  │  │ MCP     │    │   │
│  │  │ Engine  │  │ Loader  │  │ Engine  │  │ Bridge  │    │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Skill Store                              │   │
│  │  内置 Skills │ 用户 Skills │ 市场 Skills                   │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────────┐
│                       基础设施层                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ MySQL    │  │ Redis    │  │ COS      │  │ Node.js  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心设计原则

1. **Skill 是一等公民** — 不只是配置，而是可执行的单元（prompt + tools + script + MCP）
2. **Agent 是独立的推理单元** — 每个 Agent 有独立的 prompt、工具集、模型配置
3. **工作流是图，不是线性链** — 用 LangGraph4j 定义节点和边，支持条件分支和循环
4. **工具是可扩展的** — 内置工具 + Skill 自定义工具 + MCP 外部工具
5. **预览和部署分离** — 生成后立即可预览，部署是独立的上线步骤

---

## 三、详细设计

### 3.1 Skill 引擎重构

#### 3.1.1 新 Skill 数据模型

```sql
CREATE TABLE code_skill_v2 (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  skill_key      VARCHAR(50) UNIQUE NOT NULL,
  name           VARCHAR(100) NOT NULL,
  description    VARCHAR(500),
  version        VARCHAR(20) DEFAULT '1.0.0',
  author         VARCHAR(100),
  icon_url       VARCHAR(500),

  -- Prompt 配置
  system_prompt  TEXT NOT NULL,
  prompt_vars    JSON,                    -- 可注入的变量模板

  -- 工具配置
  builtin_tools  JSON,                    -- 内置工具列表 ["readFile","writeFile",...]
  custom_tools   JSON,                    -- 自定义工具定义 [{name, description, endpoint}]
  mcp_servers    JSON,                    -- MCP 服务配置 [{name, command, args}]

  -- 脚本配置
  hooks          JSON,                    -- 生命周期钩子 {beforeGenerate, afterGenerate, onError}

  -- 生成配置
  code_gen_type  VARCHAR(30) NOT NULL,
  build_strategy VARCHAR(30) DEFAULT 'none',
  model_strategy VARCHAR(30) DEFAULT 'standard',
  max_tokens     INT DEFAULT 8192,
  temperature    DOUBLE DEFAULT 0.7,

  -- 运营配置
  point_cost     INT DEFAULT 10,
  is_active      TINYINT DEFAULT 1,
  is_public      TINYINT DEFAULT 0,       -- 是否公开到市场
  sort_order     INT DEFAULT 0,
  use_count      INT DEFAULT 0,

  -- 元数据
  content_hash   VARCHAR(64),
  source         VARCHAR(20) DEFAULT 'manual',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete      TINYINT DEFAULT 0
);
```

#### 3.1.2 Skill 目录结构

```
skills/
  landing-page/
    SKILL.md              # 元数据 + system prompt
    hooks/
      before-generate.js  # 生成前钩子（可选）
      after-generate.js   # 生成后钩子（可选）
    tools/
      qr-generator.js     # 自定义工具（可选）
    templates/
      default.html        # 模板文件（可选）
```

#### 3.1.3 Skill 生命周期钩子

```javascript
// hooks/before-generate.js
// 在 AI 生成前执行，可以预处理用户输入、收集素材等
module.exports = async (context) => {
  const { userMessage, appId, tools } = context;
  // 调用外部 API 收集素材
  const images = await tools.searchImages(userMessage);
  return { ...context, images };
};

// hooks/after-generate.js
// 在 AI 生成后执行，可以后处理代码、添加额外文件等
module.exports = async (context) => {
  const { generatedCode, appId } = context;
  // 添加 analytics 脚本
  const analytics = '<script src="analytics.js"></script>';
  return generatedCode.replace('</head>', analytics + '</head>');
};
```

#### 3.1.4 自定义工具定义

```json
{
  "name": "qrGenerator",
  "description": "生成二维码图片 URL",
  "parameters": {
    "type": "object",
    "properties": {
      "text": { "type": "string", "description": "要编码的文本" },
      "size": { "type": "integer", "description": "尺寸", "default": 200 }
    },
    "required": ["text"]
  },
  "endpoint": "https://api.qrserver.com/v1/create-qr-code/?size={size}x{size}&data={text}",
  "method": "GET",
  "responseExtractor": "url"
}
```

---

### 3.2 Agent 编排层

#### 3.2.1 Agent 定义

每个 Agent 是一个独立的推理单元：

```java
public interface Agent {
    String getName();
    String getSystemPrompt();
    List<String> getTools();
    String getModelStrategy();
    AgentResult execute(AgentContext context);
}
```

#### 3.2.2 内置 Agents

| Agent | 职责 | 输入 | 输出 |
|-------|------|------|------|
| **RouterAgent** | 分析需求，选择 Skill | 用户消息 | skillKey + 理由 |
| **GeneratorAgent** | 生成代码 | 用户消息 + Skill 配置 + 图片素材 | 代码文件列表 |
| **ReviewerAgent** | 审查代码质量 | 代码内容 | ReviewResult (pass/fail + issues) |
| **OptimizerAgent** | 优化代码 | 代码 + 审查意见 | 优化后的代码 |
| **ImageCollectorAgent** | 收集图片素材 | 用户消息 | 图片 URL 列表 |

#### 3.2.3 LangGraph4j 工作流

```java
public class MainWorkflow {
    public CompiledGraph<WorkflowState> create() {
        return new StateGraph<WorkflowState>()
            // 节点
            .addNode("router", routerAgent)
            .addNode("image_collector", imageCollectorAgent)
            .addNode("generator", generatorAgent)
            .addNode("reviewer", reviewerAgent)
            .addNode("optimizer", optimizerAgent)
            .addNode("builder", projectBuilder)

            // 边
            .addEdge(START, "router")
            .addEdge("router", "image_collector")
            .addEdge("image_collector", "generator")
            .addEdge("generator", "reviewer")

            // 条件边：审查结果
            .addConditionalEdges("reviewer",
                edge_async(state -> {
                    ReviewResult result = state.getReviewResult();
                    if (result.isPassed()) return "build";
                    if (result.getRetryCount() >= 3) return "build"; // 最多重试3次
                    return "optimizer";
                }),
                Map.of(
                    "build", "builder",
                    "optimizer", "optimizer"
                ))

            // 优化后重新审查
            .addEdge("optimizer", "reviewer")

            // 构建后结束
            .addEdge("builder", END)

            .compile();
    }
}
```

#### 3.2.4 工作流状态

```java
@Data
public class WorkflowState {
    private String appId;
    private String userMessage;
    private String skillKey;
    private CodeSkill skill;

    // Router 输出
    private String selectedSkillKey;
    private String routingReason;

    // ImageCollector 输出
    private List<ImageResource> images;

    // Generator 输出
    private String generatedCode;
    private Map<String, String> generatedFiles; // path -> content

    // Reviewer 输出
    private ReviewResult reviewResult;
    private int reviewRetryCount;

    // Optimizer 输出
    private String optimizedCode;

    // Builder 输出
    private String buildStatus;
    private String deployUrl;

    // 监控
    private MonitorContext monitorContext;
    private long startTime;
}
```

---

### 3.3 工具注册表

#### 3.3.1 工具来源

```
┌─────────────────────────────────────────────────┐
│                 Tool Registry                    │
│                                                  │
│  ┌─────────────┐  内置工具（22个）                │
│  │ BuiltinTool │  readFile, writeFile, modifyFile │
│  │             │  readDir, deleteFile, exit       │
│  │             │  webSearch, webFetch             │
│  │             │  searchContentImages, ...        │
│  └─────────────┘                                 │
│                                                  │
│  ┌─────────────┐  Skill 自定义工具               │
│  │ CustomTool  │  通过 HTTP endpoint 调用         │
│  │             │  JSON schema 定义参数             │
│  └─────────────┘                                 │
│                                                  │
│  ┌─────────────┐  MCP 外部工具                   │
│  │ McpTool     │  通过 MCP 协议调用               │
│  │             │  支持 stdio / SSE 传输           │
│  └─────────────┘                                 │
└─────────────────────────────────────────────────┘
```

#### 3.3.2 工具注册接口

```java
public interface ToolRegistry {
    // 注册工具
    void register(ToolDefinition definition);

    // 按名称获取
    ToolDefinition get(String name);

    // 按 Skill 获取可用工具
    List<ToolDefinition> getToolsForSkill(String skillKey);

    // 获取所有工具
    List<ToolDefinition> getAll();
}

@Data
public class ToolDefinition {
    private String name;
    private String description;
    private String category;        // builtin / custom / mcp
    private JsonSchema parameters;
    private ToolExecutor executor;
}
```

#### 3.3.3 MCP 集成

```java
@Component
public class McpToolBridge implements ToolExecutor {
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();

    public void registerServer(McpServerConfig config) {
        McpClient client = McpClient.create(config);
        clients.put(config.getName(), client);
        // 自动发现并注册该服务的所有工具
        client.listTools().forEach(tool -> {
            toolRegistry.register(new ToolDefinition()
                .setName(tool.name())
                .setDescription(tool.description())
                .setCategory("mcp")
                .setExecutor(this));
        });
    }

    @Override
    public Object execute(String toolName, Map<String, Object> params) {
        McpClient client = findClient(toolName);
        return client.callTool(toolName, params);
    }
}
```

---

### 3.4 前端改造

#### 3.4.1 新增页面

| 页面 | 路径 | 功能 |
|------|------|------|
| Skill 市场 | `/skills` | 浏览/搜索/安装公开 Skill |
| Skill 详情 | `/skills/:key` | 查看 Skill 详情/预览/安装 |
| Skill 编辑器 | `/skills/edit/:key?` | 创建/编辑 Skill（含 prompt 编辑器/工具配置/钩子脚本） |
| Agent 监控 | `/admin/agents` | 查看 Agent 执行状态/日志/性能 |

#### 3.4.2 Skill 编辑器组件

```
┌─────────────────────────────────────────────────────────┐
│ Skill 编辑器                                              │
├─────────────────────────────────────────────────────────┤
│ [基本信息]  [Prompt]  [工具]  [钩子]  [配置]              │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ ┌── 基本信息 ──────────────────────────────────────────┐ │
│ │ 名称: [____________]  标识: [____________]            │ │
│ │ 描述: [________________________________]              │ │
│ │ 图标: [上传]  版本: [1.0.0]  作者: [____]             │ │
│ └──────────────────────────────────────────────────────┘ │
│                                                          │
│ ┌── Prompt 编辑器 ─────────────────────────────────────┐ │
│ │ ┌──────────────────────────────────────────────────┐ │ │
│ │ │ 你是一位资深的 {{role}} 工程师...                  │ │ │
│ │ │                                                  │ │ │
│ │ │ ## 技术栈                                        │ │ │
│ │ │ {{techStack}}                                    │ │ │
│ │ │                                                  │ │ │
│ │ │ ## 输出要求                                      │ │ │
│ │ │ 使用 writeFile 工具写入所有文件                   │ │ │
│ │ └──────────────────────────────────────────────────┘ │ │
│ │ 变量: [role] [techStack] [+ 添加变量]                │ │
│ └──────────────────────────────────────────────────────┘ │
│                                                          │
│ ┌── 工具配置 ─────────────────────────────────────────┐ │
│ │ 内置工具: ☑readFile ☑writeFile ☑modifyFile ☑exit    │ │
│ │ 自定义工具: [qrGenerator] [+]                        │ │
│ │ MCP 服务: [chrome-devtools] [+]                      │ │
│ └──────────────────────────────────────────────────────┘ │
│                                                          │
│ [保存草稿]  [发布到市场]  [测试生成]                      │
└─────────────────────────────────────────────────────────┘
```

#### 3.4.3 Skill 市场页面

```
┌─────────────────────────────────────────────────────────┐
│ Skill 市场                                    [创建 Skill] │
├─────────────────────────────────────────────────────────┤
│ [搜索...]  [全部] [前端] [全栈] [工具] [其他]             │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│ │ 🎨       │  │ 📊       │  │ 🛒       │               │
│ │ 着陆页   │  │ 管理后台 │  │ 电商模板 │               │
│ │ v2.1     │  │ v1.5     │  │ v1.0     │               │
│ │ ⭐ 4.8   │  │ ⭐ 4.6   │  │ ⭐ 4.9   │               │
│ │ 1.2k 次  │  │ 856 次   │  │ 2.1k 次  │               │
│ │ [安装]   │  │ [安装]   │  │ [安装]   │               │
│ └──────────┘  └──────────┘  └──────────┘               │
│                                                          │
│ ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│ │ 📱       │  │ 🎮       │  │ 📝       │               │
│ │ 移动端H5 │  │ 游戏官网 │  │ 博客模板 │               │
│ │ v1.2     │  │ v1.0     │  │ v2.0     │               │
│ │ ⭐ 4.7   │  │ ⭐ 4.5   │  │ ⭐ 4.8   │               │
│ │ 650 次   │  │ 320 次   │  │ 1.8k 次  │               │
│ │ [安装]   │  │ [安装]   │  │ [安装]   │               │
│ └──────────┘  └──────────┘  └──────────┘               │
└─────────────────────────────────────────────────────────┘
```

---

### 3.5 部署系统优化

#### 3.5.1 预览与部署分离

```
代码生成完成
    │
    ├──▶ 立即可预览（/api/static/preview/{appId}）
    │    无需部署，直接从 code_output 提供文件
    │
    └──▶ 用户点击"部署"
         ├── 复制文件到 code_deploy
         ├── 修复资源路径（绝对→相对）
         ├── 触发构建（Vue/React/全栈）
         └── 生成公网可访问 URL
```

#### 3.5.2 部署状态机

```
[草稿] ──生成代码──▶ [已生成] ──点击部署──▶ [部署中] ──成功──▶ [已上线]
                         │                      │
                         └──继续迭代──▶ [已生成]  └──失败──▶ [部署失败]
```

#### 3.5.3 部署配置

```yaml
deploy:
  # 静态资源服务
  static:
    root-dir: /tmp/code_deploy
    preview-prefix: /api/static/preview
    deploy-prefix: /api/static

  # 构建配置
  build:
    vue:
      command: npm run build
      timeout: 300s
      base-path: ./
    fullstack:
      frontend-build: npm run build
      backend-start: node index.js
      timeout: 600s

  # 外网部署（可选）
  external:
    enabled: false
    provider: nginx  # nginx / caddy / cloudflare
    domain: ${DEPLOY_DOMAIN}
```

---

### 3.6 监控与可观测性

#### 3.6.1 Agent 执行追踪

```java
@Data
public class AgentTrace {
    private String traceId;        // 全链路追踪 ID
    private String agentName;      // Agent 名称
    private String appId;          // 应用 ID
    private String userId;         // 用户 ID
    private long startTime;        // 开始时间
    private long endTime;          // 结束时间
    private int inputTokens;       // 输入 Token
    private int outputTokens;      // 输出 Token
    private String modelName;      // 使用的模型
    private String status;         // success / error / timeout
    private String errorMessage;   // 错误信息
    private Map<String, Object> metadata;  // 额外元数据
}
```

#### 3.6.2 Prometheus 指标扩展

```yaml
# 新增指标
agent_execution_total{agent_name, status}           # Agent 执行次数
agent_execution_duration_seconds{agent_name}         # Agent 执行耗时
agent_token_usage_total{agent_name, token_type}      # Agent Token 消耗
skill_usage_total{skill_key}                         # Skill 使用次数
skill_generation_duration_seconds{skill_key}         # Skill 生成耗时
tool_call_total{tool_name, status}                   # 工具调用次数
tool_call_duration_seconds{tool_name}                # 工具调用耗时
workflow_node_duration_seconds{node_name}            # 工作流节点耗时
```

#### 3.6.3 监控面板 (Grafana)

```
┌─────────────────────────────────────────────────────────┐
│ 熵擎 AI 监控面板                                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│ ┌── 概览 ──────────────┐  ┌── Agent 执行 ──────────────┐ │
│ │ 今日生成: 156        │  │ Router:    156 次, 0.2s    │ │
│ │ 活跃用户: 42         │  │ Generator: 156 次, 45s     │ │
│ │ Token 消耗: 2.3M     │  │ Reviewer:  156 次, 8s      │ │
│ │ 成功率: 94.2%        │  │ Optimizer: 23 次, 12s      │ │
│ └──────────────────────┘  └────────────────────────────┘ │
│                                                          │
│ ┌── Skill 使用分布 ────────────────────────────────────┐ │
│ │ ████████████████████ vue_project (45%)               │ │
│ │ ██████████ fullstack (22%)                           │ │
│ │ ██████ landing_page (15%)                            │ │
│ │ ████ html (10%)                                      │ │
│ │ ██ react_ts (5%)                                     │ │
│ │ █ nextjs (3%)                                        │ │
│ └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 四、优化清单（基于当前代码）

### 4.1 高优先级

| # | 优化项 | 涉及文件 | 说明 |
|---|--------|----------|------|
| 1 | 多 Agent 接入所有生成路径 | `AiCodeGeneratorFacade.java` | 当前只有 `processCodeStream` 调用了 `AgentOrchestrator`，`processTokenStream`（Vue/React/全栈）没有 |
| 2 | LangGraph4j 工作流替代手写流程 | `AiCodeGeneratorFacade.java` | 用 `CodeGenWorkflow` 替代手写的 `processCodeStream` + `processTokenStream` |
| 3 | Skill 支持自定义工具 | `CodeSkill.java`, `AiCodeGeneratorServiceFactory.java` | 通过 HTTP endpoint 定义工具，运行时动态注册 |
| 4 | 部署前重新构建 | `AppServiceImpl.java` | 全栈项目部署时重新 `npm build`，确保 `dist` 是最新的（已修复） |

### 4.2 中优先级

| # | 优化项 | 说明 |
|---|--------|------|
| 5 | Skill 脚本引擎 | 支持 `beforeGenerate` / `afterGenerate` 生命周期钩子 |
| 6 | MCP 工具集成 | 让 Skill 可以调用外部 MCP 服务（如 chrome-devtools-mcp） |
| 7 | Skill 市场 | 用户可以发布/安装/评分公开 Skill |
| 8 | 模型路由优化 | 不同 Skill 用不同模型，支持模型降级 |
| 9 | 代码生成缓存 | 相同 prompt + Skill 的生成结果可缓存 |

### 4.3 低优先级

| # | 优化项 | 说明 |
|---|--------|------|
| 10 | Skill 版本管理 | 支持 Skill 版本号、回滚、灰度发布 |
| 11 | 多语言 Skill | Skill prompt 支持 i18n |
| 12 | Skill 组合 | 多个 Skill 协同工作（如"着陆页" + "SEO 优化"） |
| 13 | 实时协作 | 多人同时编辑同一个应用 |

---

## 五、实施路线图

### Phase 1: Agent 接入（1-2 周）

```
目标: 所有生成路径都经过 Agent 审查

工作:
1. 重构 AiCodeGeneratorFacade，统一走 LangGraph4j 工作流
2. ReviewAgent 和 OptimizerAgent 接入所有 buildStrategy
3. 完善 Agent 执行追踪和日志
4. 前端显示 Agent 审查状态

验收标准:
- 所有类型的代码生成都经过审查
- 审查不通过时自动重试（最多 3 次）
- 前端能看到审查进度
```

### Phase 2: Skill 引擎（2-3 周）

```
目标: Skill 支持自定义工具和生命周期钩子

工作:
1. 扩展 code_skill 表，新增 custom_tools 和 hooks 字段
2. 实现 CustomTool 运行时（HTTP endpoint 调用）
3. 实现 ScriptEngine（GraalVM JS 或 Nashorn）
4. Skill 编辑器前端页面
5. 迁移现有 Skill 到新格式

验收标准:
- 可以通过 JSON 定义自定义工具
- 可以在 Skill 中编写 beforeGenerate/afterGenerate 钩子
- 前端可以创建和编辑 Skill
```

### Phase 3: Skill 市场（2-3 周）

```
目标: 用户可以发布和安装公开 Skill

工作:
1. Skill 发布/审核流程
2. Skill 市场前端页面
3. Skill 安装/卸载/更新机制
4. Skill 评分和评论
5. Skill 搜索和推荐

验收标准:
- 用户可以将自己的 Skill 发布到市场
- 其他用户可以搜索、安装、使用公开 Skill
- 支持 Skill 评分和使用统计
```

### Phase 4: MCP 集成（1-2 周）

```
目标: Skill 可以调用外部 MCP 服务

工作:
1. 实现 MCP Client（stdio + SSE 传输）
2. MCP 工具自动发现和注册
3. Skill 配置中声明 MCP 服务
4. 管理后台 MCP 服务管理页面

验收标准:
- 可以在 Skill 中配置 MCP 服务
- MCP 工具自动注册到工具注册表
- AI 可以调用 MCP 工具
```

### Phase 5: 监控增强（1 周）

```
目标: 完整的 Agent 执行追踪和性能监控

工作:
1. AgentTrace 实现和持久化
2. Prometheus 指标扩展
3. Grafana 监控面板
4. 告警规则配置

验收标准:
- 可以追踪每个 Agent 的执行耗时和 Token 消耗
- Grafana 面板显示实时监控数据
- 异常时自动告警
```

---

## 六、技术决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| 工作流引擎 | LangGraph4j | 已有依赖，支持条件分支和循环，Java 生态 |
| 脚本引擎 | GraalVM JS | 支持 ES6+，性能好，与 Java 互操作 |
| MCP 实现 | 自研 Client | LangChain4j 的 MCP 支持还不成熟 |
| Skill 市场 | 自建 | 不依赖第三方平台，完全可控 |
| 监控方案 | Prometheus + Grafana | 已有基础设施，社区成熟 |

---

## 七、风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|:----:|:----:|----------|
| Agent 调用链过长导致延迟增加 | 高 | 中 | 设置超时，异步执行非关键 Agent |
| 自定义工具安全风险 | 中 | 高 | 沙箱执行，限制网络访问，代码审查 |
| Skill 质量参差不齐 | 中 | 中 | 审核机制 + 评分系统 + 使用统计 |
| 模型 API 限流 | 中 | 中 | 多模型降级 + 请求队列 + 本地缓存 |
| MCP 服务稳定性 | 低 | 中 | 健康检查 + 自动重连 + 超时处理 |

---

## 附录 A: 数据库 ER 图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    user      │       │     app     │       │ chat_history │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id           │◄──┐   │ id          │◄──┐   │ id           │
│ userAccount  │   └──│ userId      │   └──│ appId        │
│ userPassword │       │ appName     │       │ userId       │
│ integral     │       │ codeGenType │       │ message      │
│ vipExpireTime│       │ deployKey   │       │ messageType  │
└─────────────┘       │ initPrompt  │       └─────────────┘
                       └─────────────┘
                              │
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│ code_skill   │       │token_usage  │       │user_quota    │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id           │       │ id          │       │ id           │
│ skill_key    │       │ userId      │       │ user_role    │
│ system_prompt│       │ appId       │       │ daily_limit  │
│ tool_names   │       │ modelName   │       │ monthly_limit│
│ build_strategy│      │ inputTokens │       └─────────────┘
│ model_strategy│      │ outputTokens│
│ custom_tools │       └─────────────┘
│ hooks        │
└─────────────┘
```

---

## 附录 B: API 接口清单（新增）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/skill/market` | 获取公开 Skill 列表（分页） |
| POST | `/api/skill/publish` | 发布 Skill 到市场 |
| POST | `/api/skill/install` | 安装公开 Skill |
| POST | `/api/skill/uninstall` | 卸载 Skill |
| GET | `/api/skill/:key/versions` | 获取 Skill 版本历史 |
| POST | `/api/skill/:key/review` | 提交 Skill 审核 |
| GET | `/api/agent/traces` | 获取 Agent 执行追踪 |
| GET | `/api/agent/traces/:traceId` | 获取单次追踪详情 |
| GET | `/api/agent/stats` | 获取 Agent 统计 |
| POST | `/api/mcp/register` | 注册 MCP 服务 |
| GET | `/api/mcp/servers` | 获取已注册的 MCP 服务 |
| GET | `/api/mcp/servers/:name/tools` | 获取 MCP 服务的工具列表 |

---

*文档结束。后续实施请严格按照此文档执行。*
