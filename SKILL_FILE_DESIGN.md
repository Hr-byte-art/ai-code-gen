# 文件化 Skill 设计方案

## 1. 背景

当前 Skill 定义分散在两处：
- 4 个 Skill 的 prompt 在 `resources/prompt/*.txt` 文件中
- 7 个 Skill 的 prompt 硬编码在 `CodeSkillDataInitializer.java` 的 Java 字符串常量中（几百行）

改一个 prompt 要改 Java 代码、重新编译。无法版本管理 prompt 变更历史。

**目标**：将 Skill 定义统一为 Markdown 文件，启动时自动同步到数据库。文件是 source of truth，数据库是运行时缓存。

---

## 2. 文件格式

每个 Skill 一个目录，目录下放 `SKILL.md`：

```
backend/src/main/resources/skills/
├── html/
│   └── SKILL.md
├── multi-file/
│   └── SKILL.md
├── vue-project/
│   └── SKILL.md
├── fullstack/
│   └── SKILL.md
├── react-ts/
│   └── SKILL.md
├── nextjs/
│   └── SKILL.md
├── landing-page/
│   └── SKILL.md
├── admin-dashboard/
│   └── SKILL.md
├── nodejs-api/
│   └── SKILL.md
├── skill-manager/
│   └── SKILL.md
└── auto/
    └── SKILL.md
```

### SKILL.md 格式

```markdown
---
name: "Vue 工程"
skillKey: vue_project
description: "使用 Vue 3 + Vite 构建完整的前端工程"
codeGenType: vue_project
pointCost: 15
toolNames: ""
buildStrategy: vue
modelStrategy: reasoning
sortOrder: 30
isActive: true
---

（以下是 systemPrompt 内容，直接写 Markdown）

你是一位资深的 Vue3 前端架构师...
```

**规则：**
- `---` 之间的 YAML frontmatter 定义元数据
- `---` 之后的所有内容就是 `systemPrompt`
- YAML 中的字符串字段用双引号包裹
- `toolNames` 为空字符串表示使用默认工具集
- `isActive` 可选，默认 `true`
- `sortOrder` 可选，默认按目录名字母序

---

## 3. 同步机制

### 3.1 启动时同步

```
应用启动
  │
  ▼
SkillFileLoader.loadAll()
  │  扫描 skills/*/SKILL.md
  │  解析 YAML frontmatter + Markdown body
  │  生成 List<CodeSkill>
  │
  ▼
SkillSyncService.sync()
  │
  ├── 文件中有，DB 中无 → INSERT
  ├── 文件中有，DB 中有，内容不同 → UPDATE（以文件为准）
  ├── DB 中有，文件中无 → 标记 isDelete=1（软删除）
  └── 内容相同 → 跳过
  │
  ▼
Caffeine 缓存刷新
```

### 3.2 内容变更检测

用 `systemPrompt` 的 MD5 哈希判断是否需要更新：
- 文件中记录一个 `contentHash` 字段（新增列）
- 启动时比较 hash，不同才 UPDATE
- 避免每次启动都全量更新

### 3.3 热重载（可选，Phase 2）

监听 `skills/` 目录变更（WatchService），文件修改后自动重新加载，无需重启。

---

## 4. 数据库变更

### 4.1 新增列

```sql
ALTER TABLE code_skill ADD COLUMN content_hash VARCHAR(64) DEFAULT NULL;
ALTER TABLE code_skill ADD COLUMN source VARCHAR(20) DEFAULT 'file';
-- source: 'file' = 来自文件, 'manual' = 手动创建（管理员通过 UI 添加）
```

### 4.2 数据来源标记

| source | 含义 | 行为 |
|--------|------|------|
| `file` | 来自 SKILL.md 文件 | 启动时以文件为准同步 |
| `manual` | 管理员通过 UI 手动创建 | 不受文件同步影响，保留在 DB 中 |

---

## 5. 核心类设计

### 5.1 SkillFileLoader

```java
@Component
public class SkillFileLoader {

    private static final String SKILLS_DIR = "skills/";

    /**
     * 扫描 classpath 下 skills/*/SKILL.md，解析为 CodeSkill 列表
     */
    public List<CodeSkill> loadAll() {
        // 1. 读取 skills/ 目录下所有子目录
        // 2. 每个子目录找 SKILL.md
        // 3. 解析 YAML frontmatter → CodeSkill 元数据
        // 4. 解析 Markdown body → systemPrompt
        // 5. 计算 contentHash
    }

    /**
     * 解析单个 SKILL.md 文件
     */
    public CodeSkill parse(Path skillMdPath) {
        String content = Files.readString(skillMdPath);
        String[] parts = content.split("---\n", 3);
        // parts[0] = 空, parts[1] = YAML, parts[2] = Markdown body

        Map<String, Object> yaml = new Yaml().load(parts[1]);
        String systemPrompt = parts[2].trim();

        return CodeSkill.builder()
                .skillKey((String) yaml.get("skillKey"))
                .name((String) yaml.get("name"))
                .description((String) yaml.get("description"))
                .codeGenType((String) yaml.get("codeGenType"))
                .pointCost((Integer) yaml.get("pointCost"))
                .toolNames((String) yaml.get("toolNames"))
                .buildStrategy((String) yaml.get("buildStrategy"))
                .modelStrategy((String) yaml.get("modelStrategy"))
                .sortOrder((Integer) yaml.get("sortOrder"))
                .isActive(yaml.containsKey("isActive") ? (Boolean) yaml.get("isActive") ? 1 : 0 : 1)
                .systemPrompt(systemPrompt)
                .contentHash(md5(systemPrompt))
                .source("file")
                .build();
    }
}
```

### 5.2 SkillSyncService

```java
@Component
public class SkillSyncService {

    @Resource
    private SkillFileLoader fileLoader;
    @Resource
    private CodeSkillService codeSkillService;

    /**
     * 启动时同步：文件 → DB
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        List<CodeSkill> fileSkills = fileLoader.loadAll();
        List<CodeSkill> dbSkills = codeSkillService.list();

        Map<String, CodeSkill> fileMap = fileSkills.stream()
                .collect(Collectors.toMap(CodeSkill::getSkillKey, s -> s));
        Map<String, CodeSkill> dbMap = dbSkills.stream()
                .collect(Collectors.toMap(CodeSkill::getSkillKey, s -> s));

        // 文件中有 → INSERT 或 UPDATE
        for (var entry : fileMap.entrySet()) {
            String key = entry.getKey();
            CodeSkill fileSkill = entry.getValue();
            CodeSkill dbSkill = dbMap.get(key);

            if (dbSkill == null) {
                codeSkillService.save(fileSkill); // INSERT
            } else if (!dbSkill.getContentHash().equals(fileSkill.getContentHash())) {
                fileSkill.setId(dbSkill.getId());
                codeSkillService.updateById(fileSkill); // UPDATE
            }
        }

        // DB 中有但文件中无（且 source=file）→ 软删除
        for (var entry : dbMap.entrySet()) {
            if (!fileMap.containsKey(entry.getKey()) && "file".equals(entry.getValue().getSource())) {
                entry.getValue().setIsDelete(1);
                codeSkillService.updateById(entry.getValue());
            }
        }
    }
}
```

### 5.3 CodeSkillService 增强

```java
// 新增方法
public interface CodeSkillService extends IService<CodeSkill> {
    // ... 现有方法 ...

    /**
     * 从缓存获取（优先缓存，fallback DB）
     */
    CodeSkill getByCodeGenTypeCached(String codeGenType);
}
```

加一层 Caffeine 缓存，避免每次 `getByCodeGenType()` 都查 DB：

```java
private final Cache<String, CodeSkill> cache = Caffeine.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();

@Override
public CodeSkill getByCodeGenTypeCached(String codeGenType) {
    return cache.get(codeGenType, key -> getByCodeGenType(key));
}
```

---

## 6. 代码生成流程变更

将 `AiCodeGeneratorServiceFactory` 和 `AppServiceImpl` 中的 `codeSkillService.getByCodeGenType()` 替换为 `codeSkillService.getByCodeGenTypeCached()`。

```
AppServiceImpl.chatToGenCode()
  → codeSkillService.getByCodeGenTypeCached("vue_project")  // 带缓存
  → aiCodeGeneratorFacade.generateAndSaveCodeStream(message, skill, appId)
```

无其他流程变更。现有的 AI 服务创建、工具注入、构建流程完全不变。

---

## 7. Admin UI 兼容

| 操作 | 行为 |
|------|------|
| 查看列表 | 正常展示（file + manual 来源都显示） |
| 编辑 file 来源的 Skill | 提示"此 Skill 来自文件，编辑后将在下次启动时被文件覆盖" |
| 编辑 manual 来源的 Skill | 正常编辑，保存到 DB |
| 新增 Skill | 默认 source=manual，保存到 DB |
| 删除 file 来源的 Skill | 提示"请删除对应的 SKILL.md 文件" |
| 删除 manual 来源的 Skill | 正常软删除 |

---

## 8. 迁移步骤

### Phase 1：创建文件（1 天）

- [ ] 创建 `skills/` 目录结构
- [ ] 将 4 个已有 `.txt` prompt 文件转为 `SKILL.md` 格式
- [ ] 将 7 个 Java inline prompt 常量提取为 `SKILL.md` 文件
- [ ] 验证 11 个 SKILL.md 文件格式正确

### Phase 2：加载器（1 天）

- [ ] 实现 `SkillFileLoader`（YAML frontmatter 解析）
- [ ] 实现 `SkillSyncService`（启动同步逻辑）
- [ ] 数据库加 `content_hash` 和 `source` 列

### Phase 3：缓存 + 集成（1 天）

- [ ] `CodeSkillService` 加 Caffeine 缓存
- [ ] 替换调用方为 `getByCodeGenTypeCached()`
- [ ] 删除 `CodeSkillDataInitializer` 中的 inline prompt 常量

### Phase 4：清理（0.5 天）

- [ ] 删除旧的 `resources/prompt/codegen-*-system-prompt.txt` 文件
- [ ] 删除 `CodeSkillDataInitializer`（被 `SkillSyncService` 替代）
- [ ] 更新 CLAUDE.md 文档

---

## 9. 最终目录结构

```
backend/src/main/resources/
├── skills/                          ← 新增：所有 Skill 定义
│   ├── html/SKILL.md
│   ├── multi-file/SKILL.md
│   ├── vue-project/SKILL.md
│   ├── fullstack/SKILL.md
│   ├── react-ts/SKILL.md
│   ├── nextjs/SKILL.md
│   ├── landing-page/SKILL.md
│   ├── admin-dashboard/SKILL.md
│   ├── nodejs-api/SKILL.md
│   ├── skill-manager/SKILL.md
│   └── auto/SKILL.md
├── prompt/                          ← 保留：非 Skill 的 prompt
│   ├── codegen-routing-system-prompt.txt
│   ├── image-collection-plan-system-prompt.txt
│   └── image-collection-system-prompt.txt
└── ...
```

---

## 10. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| YAML 解析失败 | Skill 加载失败 | 严格校验 + 启动日志报错 + fallback 到 DB |
| 文件编码问题 | 中文乱码 | 统一 UTF-8，`Files.readString()` 默认 UTF-8 |
| 文件被误删 | Skill 消失 | DB 中保留记录，标记 isDelete=1，日志警告 |
| 缓存与文件不一致 | 用了旧 prompt | 启动时强制同步 + 缓存 TTL 10 分钟 |
| Admin UI 编辑 file 来源 | 改动被覆盖 | UI 提示来源 + 编辑时弹警告 |
