# 公共 API 集成设计方案

## 1. 背景与目标

当前平台已集成 Pexels（图片）、Igoutu（插画）、Kroki（图表）等外部 API，但生成的项目缺乏常用的通用能力（图表、词典、二维码、颜色等）。

本方案从 [public-apis](https://github.com/public-apis/public-apis) 中筛选出 **无需认证、免费、支持 HTTPS 和 CORS** 的 API，分两层集成：

- **平台层**：增强平台自身功能（截图、图表预览）
- **工具层**：注册为 AI Tool，让代码生成时可调用（词典、颜色、二维码等）

---

## 2. 候选 API 清单

### 2.1 平台增强类

| API | 端点 | 用途 | 集成位置 |
|-----|------|------|----------|
| QuickChart | `https://quickchart.io/chart` | 通过 URL 参数生成图表图片，支持 Chart.js 语法 | Prompt 模板 + AI 可选引用 |
| Kroki (已有) | `https://kroki.io` | 文本转架构图/流程图 | 已集成 MermaidDiagramTool |
| JSONPlaceholder | `https://jsonplaceholder.typicode.com` | 6 个资源端点的假数据 REST API | 新增 MockApiTool |
| **Openverse** | `https://api.openverse.org/v1/images/` | WordPress 开放许可图片搜索引擎，7 亿+ 图片 | ImageSearchTool 并行搜索 |

### 2.2 图片搜索：多源并行策略

当前平台仅使用 Pexels 作为图片搜索源。Pexels 风景照和高质量库存图片占优，但覆盖面有限。

**策略：Pexels + Openverse 并行请求，结果合并去重**

```
用户关键词 "海滩日落"
        │
        ├──────────────────┐
        ▼                  ▼
   Pexels API         Openverse API
   (风景照强)         (覆盖面广)
   返回 4 张           返回 4 张
        │                  │
        └────────┬─────────┘
                 ▼
          合并 + 去重 + 质量排序
                 │
                 ▼
          取 Top 6 交给 AI
```

| 源 | 优势 | 图片风格 | 认证 | 免费额度 |
|----|------|----------|------|----------|
| Pexels（已有） | 高质量、风景/人像强 | 专业摄影 | API Key | 合理使用不限 |
| Openverse（新增） | 7 亿+聚合、覆盖面广 | 多元（摄影/插画/矢量） | 无需 | 100 次/分钟 |

**合并策略**：
1. 两个请求用 `CompletableFuture` 并行发出，超时 5s
2. 按关键词相关性 + 图片质量（分辨率、比例）打分
3. 去重（按 URL 去重）
4. 取 Top 6 返回给 AI 选择

### 2.2 AI 工具类（生成项目可用）

| API | 端点 | 用途 | 工具名 |
|-----|------|------|--------|
| Free Dictionary | `https://api.dictionaryapi.dev/api/v2/entries/en/{word}` | 英文词典查询（释义、发音、例句） | `dictionaryLookup` |
| xColors | `https://x-colors.yurace.pro/api/random` | 颜色生成、转换、调色板 | `colorGenerator` |
| QR code (goqr) | `https://api.qrserver.com/v1/create-qr-code/` | 二维码生成 | `qrCodeGenerator` |
| PHP-Noise | `https://php-noise.com/noise.php` | 噪点背景图生成 | `noiseBackground` |
| CountAPI | `https://api.countapi.xyz` | 页面访问计数 | `pageCounter` |
| Icon Horse | `https://favicon.iconhorse.dev/` | 获取任意网站 favicon | `faviconFetcher` |
| Bored | `https://bored-api.appbrewery.com/random` | 随机活动推荐（创意类 app 用） | `randomActivity` |

---

## 3. 架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    AI 代码生成引擎                        │
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │  System      │    │  Tool       │    │  Prompt     │  │
│  │  Prompt      │    │  Manager    │    │  Template   │  │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘  │
│         │                  │                  │         │
│         ▼                  ▼                  ▼         │
│  ┌──────────────────────────────────────────────────┐   │
│  │            PublicApiTool (基类)                    │   │
│  │  - baseUrl, timeout, retry                        │   │
│  │  - get(), post(), withCache()                     │   │
│  └──────────────────────────────────────────────────┘   │
│         │         │         │         │                 │
│    ┌────┴───┐ ┌───┴───┐ ┌───┴───┐ ┌───┴───┐            │
│    │DictTool│ │QR Tool│ │Color  │ │MockApi│  ...        │
│    └────────┘ └───────┘ └───────┘ └───────┘            │
└─────────────────────────────────────────────────────────┘
```

### 3.2 新增 Tool 基类

所有公共 API 工具继承 `PublicApiTool`，统一处理：

```java
public abstract class PublicApiTool extends BaseTool {
    protected static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    protected String doGet(String url) { ... }
    protected String doGetWithCache(String url, Duration ttl) { ... }
}
```

### 3.3 注册方式

与现有工具一致：继承 `BaseTool`，加 `@Component` 注解，Spring 自动发现并通过 `ToolManager` 注册。工厂根据 `CodeSkill.toolNames` 决定哪些工具注入 AI 服务。

---

## 4. 详细设计

### 4.1 DictionaryLookupTool（词典查询）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/DictionaryLookupTool.java`

```java
@Component
public class DictionaryLookupTool extends PublicApiTool {
    private static final String API = "https://api.dictionaryapi.dev/api/v2/entries/en/";

    @Tool(name = "dictionaryLookup",
          value = "查询英文单词的释义、音标、例句。输入英文单词，返回词典信息。")
    public String lookup(@P("英文单词") String word) {
        // GET https://api.dictionaryapi.dev/api/v2/entries/en/{word}
        // 解析 JSON，提取：phonetic, meanings[].partOfSpeech,
        //   meanings[].definitions[].definition, meanings[].definitions[].example
        // 返回格式化文本
    }
}
```

**用途场景**: 用户生成英语学习 app、翻译工具、词典应用时，AI 可直接调用获取真实词典数据嵌入代码。

### 4.2 QrCodeGeneratorTool（二维码生成）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/QrCodeGeneratorTool.java`

```java
@Component
public class QrCodeGeneratorTool extends BaseTool {

    @Tool(name = "qrCodeGenerator",
          value = "生成二维码图片 URL。输入文本内容，返回二维码图片链接。")
    public String generate(@P("要编码的文本或 URL") String text,
                           @P("尺寸(像素)") @Optional Integer size) {
        // 构造 URL: https://api.qrserver.com/v1/create-qr-code/?size={size}x{size}&data={text}
        // 返回图片 URL（直接可用于 <img src>）
    }
}
```

**用途场景**: 生成带二维码的落地页、名片页、活动页。

### 4.3 ColorGeneratorTool（颜色生成）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/ColorGeneratorTool.java`

```java
@Component
public class ColorGeneratorTool extends BaseTool {

    @Tool(name = "colorGenerator",
          value = "生成随机颜色或配色方案。返回 HEX 色值。可指定颜色数量。")
    public String generate(@P("需要的颜色数量") @Optional Integer count) {
        // GET https://x-colors.yurace.pro/api/random?number={count}
        // 返回 JSON: [{ hex, rgb, hsl }, ...]
    }
}
```

**用途场景**: AI 生成项目时自动获取配色方案，替代硬编码颜色值。

### 4.4 NoiseBackgroundTool（噪点背景）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/NoiseBackgroundTool.java`

```java
@Component
public class NoiseBackgroundTool extends BaseTool {

    @Tool(name = "noiseBackground",
          value = "生成噪点纹理背景图片 URL。可指定尺寸和颜色。")
    public String generate(@P("宽度") @Optional Integer width,
                           @P("高度") @Optional Integer height,
                           @P("HEX 颜色") @Optional String color) {
        // GET https://php-noise.com/noise.php?w={w}&h={h}&color={color}&tile&json
        // 返回图片 URL
    }
}
```

### 4.5 MockApiTool（模拟 API）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/MockApiTool.java`

```java
@Component
public class MockApiTool extends BaseTool {

    @Tool(name = "mockApi",
          value = "获取模拟 REST API 数据。支持 posts/comments/albums/photos/todos/users 六种资源。" +
                  "用于生成 demo 项目的后端数据。")
    public String getMockData(@P("资源类型: posts|comments|albums|photos|todos|users") String resource,
                              @P("ID (可选)") @Optional Integer id,
                              @P("数量限制") @Optional Integer limit) {
        // GET https://jsonplaceholder.typicode.com/{resource}?_limit={limit}
        // 或 GET https://jsonplaceholder.typicode.com/{resource}/{id}
    }
}
```

**用途场景**: 生成 demo 项目时，AI 可调用获取真实结构的假数据，替代空列表或硬编码数据。

### 4.6 FaviconFetcherTool（网站图标）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/FaviconFetcherTool.java`

```java
@Component
public class FaviconFetcherTool extends BaseTool {

    @Tool(name = "faviconFetcher",
          value = "获取任意网站的 favicon 图标 URL。输入域名，返回图标链接。")
    public String fetch(@P("网站域名，如 google.com") String domain) {
        // 直接返回: https://favicon.iconhorse.dev/{domain}
    }
}
```

### 4.7 PageCounterTool（访问计数）

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/PageCounterTool.java`

```java
@Component
public class PageCounterTool extends BaseTool {

    @Tool(name = "pageCounter",
          value = "创建页面访问计数器。返回一个计数器 ID，生成的项目可通过 fetch 调用获取访问次数。")
    public String create(@P("命名空间") String namespace,
                         @P("计数器名称") String key) {
        // GET https://api.countapi.xyz/hit/{namespace}/{key}
        // 返回 JSON: { value: 1 }
        // 同时提供前端调用示例代码
    }
}
```

### 4.8 多源图片搜索重构（Pexels + Openverse 并行）

**涉及文件**:
- `backend/src/main/java/com/wjh/aicodegen/langgraph4j/tools/ImageSearchTool.java` — 重构
- `backend/src/main/java/com/wjh/aicodegen/ai/tools/AiImageSearchTool.java` — 重构
- 新增 `backend/src/main/java/com/wjh/aicodegen/ai/tools/openverse/OpenverseImageSource.java`

#### 4.8.1 抽象图片源接口

```java
public interface ImageSource {
    String getName();
    List<ImageResource> search(String query, int count);
}
```

#### 4.8.2 Pexels 实现（已有逻辑迁移）

```java
@Component
public class PexelsImageSource implements ImageSource {
    @Value("${picture.search.api-key:}")
    private String apiKey;

    @Override
    public String getName() { return "pexels"; }

    @Override
    public List<ImageResource> search(String query, int count) {
        if (apiKey == null || apiKey.isBlank()) return List.of();
        // 现有 Pexels 调用逻辑迁移至此
    }
}
```

#### 4.8.3 Openverse 实现（新增）

```java
@Component
public class OpenverseImageSource implements ImageSource {
    private static final String API = "https://api.openverse.org/v1/images/";

    @Override
    public String getName() { return "openverse"; }

    @Override
    public List<ImageResource> search(String query, int count) {
        // GET https://api.openverse.org/v1/images/?q={query}&page_size={count}&license_type=commercial
        // 解析 JSON:
        //   results[].url          — 原图 URL
        //   results[].thumbnail    — 缩略图 URL
        //   results[].title        — 标题
        //   results[].creator      — 作者
        //   results[].license      — 许可证 (CC0, CC-BY, CC-BY-SA, CC-BY-NC, ...)
        //   results[].source       — 来源平台
        //
        // 过滤：只保留 license_type=commercial 的图片
        // 转换为 ImageResource 列表
    }
}
```

#### 4.8.4 并行搜索编排

```java
@Component
public class ParallelImageSearcher {

    @Autowired
    private List<ImageSource> imageSources; // Spring 自动注入所有实现

    public List<ImageResource> search(String query, int totalCount) {
        int perSource = totalCount / imageSources.size() + 1;

        // 并行请求所有源
        List<CompletableFuture<List<ImageResource>>> futures = imageSources.stream()
            .map(source -> CompletableFuture
                .supplyAsync(() -> source.search(query, perSource))
                .completeOnTimeout(List.of(), 5, TimeUnit.SECONDS) // 5s 超时
                .exceptionally(ex -> List.of())) // 异常降级为空
            .toList();

        // 等待所有完成，合并结果
        List<ImageResource> allImages = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        // 去重（按 URL）
        // 打分排序（分辨率、比例、关键词匹配度）
        // 取 Top N
        return allImages.stream()
            .collect(Collectors.toMap(ImageResource::getUrl, r -> r, (a, b) -> a))
            .values().stream()
            .sorted(Comparator.comparingInt(this::scoreImage).reversed())
            .limit(totalCount)
            .collect(Collectors.toList());
    }

    private int scoreImage(ImageResource img) {
        int score = 0;
        // 高分辨率加分
        // 16:9 / 4:3 比例加分
        // Pexels 来源略加分（质量更高）
        return score;
    }
}
```

#### 4.8.5 ImageSearchTool 重构

```java
@Component
public class ImageSearchTool {

    @Autowired
    private ParallelImageSearcher searcher;

    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        return searcher.search(query, 6);
    }
}
```

**关键设计决策**:
- `ImageSource` 接口：新增图片源只需实现接口 + 加 `@Component`，零改动现有代码
- `completeOnTimeout(5s)`：任一源超时不阻塞整体，优雅降级
- Pexels Key 为空时自动跳过，仅用 Openverse
- 许可证过滤：Openverse 只保留商用许可（CC0/CC-BY/CC-BY-SA）

---

## 5. Prompt 模板更新

### 5.1 系统 Prompt 中注入工具说明

在 `codegen-vue-project-system-prompt.txt` 和 `codegen-fullstack-system-prompt.txt` 中追加：

```
## 可用的公共 API 工具

生成项目时，你可以调用以下工具为项目注入真实功能：

- `dictionaryLookup` — 查询英文单词释义，适用于学习类、词典类应用
- `qrCodeGenerator` — 生成二维码图片 URL，适用于名片、活动页
- `colorGenerator` — 生成配色方案，替代硬编码颜色
- `noiseBackground` — 生成噪点纹理背景，用于设计感页面
- `mockApi` — 获取模拟数据，适用于 demo 项目的列表/详情页
- `faviconFetcher` — 获取网站 favicon，适用于书签、导航类应用
- `pageCounter` — 创建访问计数器，适用于个人主页、博客

使用原则：
1. 仅在用户需求明确需要时调用，不要过度使用
2. 调用结果直接嵌入生成的代码中（URL 或数据）
3. 在代码注释中标注 API 来源，方便用户后续替换为自己的 API
```

### 5.2 HTML/Multi-file 类型

对于 `codegen-html-system-prompt.txt`，由于不支持 tool 调用，在 prompt 中直接提供 API URL 模板：

```
## 可用的免费公共 API

在生成 HTML 时，可直接在代码中引用以下免费 API：

- 图表: https://quickchart.io/chart?c={chartjs_config_json}
- 二维码: https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={text}
- 噪点背景: https://php-noise.com/noise.php?w=800&h=600&tile
- Favicon: https://favicon.iconhorse.dev/{domain}

使用时在注释中标注 API 来源。
```

---

## 6. 实现计划

### Phase 1: 多源图片搜索（2 天）⭐ 优先级最高

- [ ] 定义 `ImageSource` 接口
- [ ] 将现有 Pexels 逻辑迁移到 `PexelsImageSource`
- [ ] 新增 `OpenverseImageSource`（无需 Key）
- [ ] 实现 `ParallelImageSearcher`（CompletableFuture 并行 + 5s 超时 + 合并去重）
- [ ] 重构 `ImageSearchTool` 和 `AiImageSearchTool` 注入 `ParallelImageSearcher`
- [ ] 测试：验证 Pexels+Openverse 并行搜索、超时降级、去重排序

### Phase 2: 基础设施（1 天）

- [ ] 新增 `PublicApiTool` 基类（HTTP 客户端、缓存、错误处理）
- [ ] 在 `application.yml` 添加公共 API 配置项（超时、重试）

### Phase 3: 核心工具（2 天）

- [ ] `DictionaryLookupTool` — 词典查询
- [ ] `QrCodeGeneratorTool` — 二维码生成
- [ ] `ColorGeneratorTool` — 颜色生成
- [ ] `MockApiTool` — 模拟数据

### Phase 4: 辅助工具（1 天）

- [ ] `NoiseBackgroundTool` — 噪点背景
- [ ] `FaviconFetcherTool` — 网站图标
- [ ] `PageCounterTool` — 访问计数

### Phase 5: Prompt 集成（1 天）

- [ ] 更新 Vue/Fullstack 系统 Prompt，注入工具说明
- [ ] 更新 HTML/Multi-file 系统 Prompt，注入 API URL 模板
- [ ] 更新路由 Prompt，识别相关需求类型

### Phase 6: 测试验证（1 天）

- [ ] 单元测试：每个工具的 HTTP 调用和响应解析
- [ ] 集成测试：AI 生成包含 API 调用的完整项目
- [ ] 端到端验证：生成 → 构建 → 部署 → 访问
- [ ] 端到端验证：生成 → 构建 → 部署 → 访问

---

## 7. 配置项

```yaml
# application.yml
public-api:
  enabled: true
  timeout-seconds: 5
  cache-ttl-minutes: 30
  endpoints:
    dictionary: https://api.dictionaryapi.dev/api/v2/entries/en/
    qrcode: https://api.qrserver.com/v1/create-qr-code/
    colors: https://x-colors.yurace.pro/api/random
    noise: https://php-noise.com/noise.php
    mock: https://jsonplaceholder.typicode.com
    favicon: https://favicon.iconhorse.dev
    counter: https://api.countapi.xyz
    quickchart: https://quickchart.io/chart

# 图片搜索多源配置
image-search:
  sources:
    pexels:
      enabled: true  # 需要配置 picture.search.api-key
    openverse:
      enabled: true  # 无需 Key，直接可用
      endpoint: https://api.openverse.org/v1/images/
      license-type: commercial  # 只搜索商用许可图片
  parallel:
    timeout-seconds: 5       # 单源超时
    total-results: 6          # 最终返回图片数
    per-source-count: 4       # 每个源请求的数量
```

---

## 8. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| 免费 API 限流/不可用 | 工具调用失败 | 5s 超时 + 优雅降级（返回空结果，AI 继续生成） |
| API 响应格式变更 | 解析失败 | 严格 JSON 解析 + try-catch + 返回错误描述给 AI |
| CORS 限制 | 前端直接调用受限 | 所有调用走后端代理，不暴露给浏览器 |
| 数据隐私 | 用户数据泄露给第三方 API | 仅传递非敏感数据（单词、颜色值、URL） |
| Prompt 膨胀 | 工具描述过多影响生成质量 | 工具描述精简到 1-2 行，按 skill 按需注入 |
| Openverse 100 次/分钟限流 | 高并发时图片搜索失败 | 单源超时降级 + Caffeine 本地缓存（按 query 缓存 5min） |
| Openverse 图片质量参差 | 返回低质量/不相关图片 | 合并后按分辨率/比例打分排序，Pexels 来源略加分 |

---

## 9. 后续扩展

- **有 Key 的 API**：OpenWeather（天气）、NewsAPI（新闻）等，通过配置 API Key 启用
- **自定义 API 注册**：用户可在前端添加自己的 API 端点，平台自动注册为 AI 工具
- **API 市场**：社区共享的 API 工具包，一键安装到项目
