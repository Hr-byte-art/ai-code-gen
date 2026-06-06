# 代码生成管线风险清单与修复指南

> 最后更新: 2026-05-31
> 本文档记录了代码生成管线中发现的所有风险点，供开发工具和开发者参考。

---

## 管线总览

```
用户输入 → AiCodeGeneratorFacade.generateAndSaveCodeStream()
  ├─ [buildStrategy == "none"] → Flux<String> → processCodeStream()
  │     └─ 收集完整输出 → ReviewWorkflow → CodeParserExecutor → CodeFileSaverExecutor
  ├─ [buildStrategy == "auto"] → TokenStream → processTokenStream()
  │     └─ 工具回调 → 审查 → detectBuildStrategy() → triggerBuild()
  └─ [buildStrategy == "vue"/"react"/"fullstack"] → TokenStream → processTokenStream()
        └─ 工具回调 → 审查 → triggerBuild() → BuildRetryService
```

LangGraph4j 独立工作流（另一条路径）:
```
ImageCollectorNode → PromptEnhancerNode → RouterNode → CodeGeneratorNode
  → CodeQualityCheckNode → [质检通过? → ProjectBuilderNode : 重回 CodeGeneratorNode]
```

---

## P0 — 必须修复（直接影响代码生成正确性）

### ISSUE-001: 质检异常时静默放行

**文件**: `backend/src/main/java/com/wjh/aicodegen/langgraph4j/node/CodeQualityCheckNode.java`
**行号**: 54-58

**现状代码**:
```java
} catch (Exception e) {
    log.error("代码质量检查异常: {}", e.getMessage(), e);
    qualityResult = QualityResult.builder()
            .isValid(true) // 异常直接跳到下一个步骤
            .build();
}
```

**问题**: 如果质检服务本身抛异常（网络超时、AI 返回格式不对、JSON 解析失败），代码会被直接标记为 `isValid=true`，跳过重试循环。任何质检故障都会让有问题的代码"过关"。

**修复方案**:
1. 将 `isValid(true)` 改为 `isValid(false)`
2. 在 errors 中附带异常信息，让重试机制介入
3. 区分"质检服务不可用"和"代码确实有问题"——如果连续 N 次质检服务异常，应该终止工作流而非无限重试

**修复后代码参考**:
```java
} catch (Exception e) {
    log.error("代码质量检查异常: {}", e.getMessage(), e);
    qualityResult = QualityResult.builder()
            .isValid(false)
            .errors(List.of("代码质量检查服务异常: " + e.getMessage()))
            .suggestions(List.of("请检查质检服务是否可用，或重新生成代码"))
            .build();
}
```

**注意**: 同时需要修改 `CodeGenWorkflow.routeAfterQualityCheck()` 中的重试上限逻辑（当前是 `qualityCheckCount > 5` 才抛异常），确保质检服务异常时不会无限重试。建议增加一个 `consecutiveErrorCount` 计数器，连续 2 次质检异常就终止。

---

### ISSUE-002: processCodeStream 静默吞掉保存异常

**文件**: `backend/src/main/java/com/wjh/aicodegen/core/AiCodeGeneratorFacade.java`
**行号**: 156-178

**现状代码**:
```java
.doOnComplete(() -> {
    GlobalContextStorage.removeContext(String.valueOf(appId));
    try {
        if (!taskCancellationManager.isTaskCancelled(appId)) {
            String completeCode = codeBuilder.toString();
            var reviewResult = reviewWorkflow.execute(completeCode, appId);
            String reviewedCode = reviewResult.getFinalCode();
            Object parsedResult = CodeParserExecutor.executeParser(reviewedCode, codeGenType);
            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
            log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
        } else {
            log.info("应用 {} 的任务已被取消，跳过文件保存", appId);
        }
    } catch (Exception e) {
        log.error("保存失败，应用ID: {}", appId, e);  // ← 异常被吞掉
    }
})
```

**问题**: 如果 `CodeParserExecutor` 或 `CodeFileSaverExecutor` 抛异常（正则匹配失败、磁盘写入失败），异常被 catch 后只打日志。前端 SSE 流正常 complete，用户看到"生成成功"但代码实际没有保存。

**修复方案**:
1. `processCodeStream` 需要拿到 `sink` 引用（当前用的是 `doOnComplete`，无法发送错误）
2. 改用 `Flux.create(sink -> ...)` 模式，和 `processTokenStream` 保持一致
3. 保存失败时通过 `sink.error()` 或 `sink.next(errorMessage)` 通知前端

**修复后代码参考**:
```java
private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType,
        Long appId, MonitorContext context) {
    StringBuilder codeBuilder = new StringBuilder();
    return Flux.<String>create(sink -> {
        codeStream
                .doOnNext(chunk -> {
                    codeBuilder.append(chunk);
                    sink.next(chunk);
                })
                .doOnComplete(() -> {
                    GlobalContextStorage.removeContext(String.valueOf(appId));
                    try {
                        if (!taskCancellationManager.isTaskCancelled(appId)) {
                            String completeCode = codeBuilder.toString();
                            var reviewResult = reviewWorkflow.execute(completeCode, appId);
                            String reviewedCode = reviewResult.getFinalCode();
                            Object parsedResult = CodeParserExecutor.executeParser(reviewedCode, codeGenType);
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
                        } else {
                            log.info("应用 {} 的任务已被取消，跳过文件保存", appId);
                        }
                        sink.complete();
                    } catch (Exception e) {
                        log.error("保存失败，应用ID: {}", appId, e);
                        sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR,
                                "代码保存失败: " + e.getMessage()));
                    }
                })
                .doOnError(e -> {
                    GlobalContextStorage.removeContext(String.valueOf(appId));
                    sink.error(e);
                })
                .subscribe();
    })
            .contextWrite(ctx -> {
                if (context != null) {
                    return MonitorContextHolder.putContextToReactor(ctx, context);
                }
                return ctx;
            });
}
```

---

### ISSUE-003: 质检重试时丢失原始 prompt

**文件**: `backend/src/main/java/com/wjh/aicodegen/langgraph4j/node/CodeGeneratorNode.java`
**行号**: 57-66, 81-95

**现状代码**:
```java
private static String buildUserMessage(WorkflowContext context) {
    String userMessage = context.getEnhancedPrompt();
    QualityResult qualityResult = context.getQualityResult();
    if (isQualityCheckFailed(qualityResult)) {
        // ← 覆盖了原始 prompt，AI 不知道用户要什么
        userMessage = buildErrorFixPrompt(qualityResult);
    }
    return userMessage;
}

private static String buildErrorFixPrompt(QualityResult qualityResult) {
    // 只包含错误信息，没有用户原始需求
    StringBuilder errorInfo = new StringBuilder();
    errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
    qualityResult.getErrors().forEach(error -> errorInfo.append("- ").append(error).append("\n"));
    // ...
    return errorInfo.toString();
}
```

**问题**: 质检失败后，`buildErrorFixPrompt` 只包含错误列表和修复建议，完全丢弃了用户的原始需求。AI 不知道用户要什么功能，只知道"上次有什么错误"，重新生成的代码很可能偏离用户意图。

**修复方案**: 修复 prompt 应该在原始 prompt 基础上追加错误信息，而不是替换。

**修复后代码参考**:
```java
private static String buildUserMessage(WorkflowContext context) {
    String userMessage = context.getEnhancedPrompt();
    QualityResult qualityResult = context.getQualityResult();
    if (isQualityCheckFailed(qualityResult)) {
        // 在原始 prompt 基础上追加错误修复信息
        userMessage = userMessage + buildErrorFixPrompt(qualityResult);
    }
    return userMessage;
}
```

---

## P1 — 强烈建议修复（影响生成质量或系统稳定性）

### ISSUE-004: 随机 appId 导致孤儿目录

**文件**: `backend/src/main/java/com/wjh/aicodegen/langgraph4j/node/CodeGeneratorNode.java`
**行号**: 37

**现状代码**:
```java
Long appId = RandomUtil.randomLong();
```

**问题**: 每次进入代码生成节点都生成一个随机 appId。质检失败重试时，会再次生成新的随机 appId，旧目录变成孤儿文件，磁盘持续膨胀。

**修复方案**: 在 `WorkflowContext` 初始化时生成一个稳定的 appId，整个工作流生命周期内复用。

**修复位置**: `CodeGenWorkflow.executeWorkflow()` 中初始化 `WorkflowContext` 时：
```java
WorkflowContext initialContext = WorkflowContext.builder()
        .originalPrompt(originalPrompt)
        .currentStep("初始化")
        // 在此处生成稳定的 appId
        .appId(IdUtil.getSnowflakeNextIdStr())
        .build();
```

然后 `CodeGeneratorNode` 从 context 中读取而不是随机生成：
```java
Long appId = context.getAppId();
```

---

### ISSUE-005: 正则解析器 fallback 不安全

**文件**: `backend/src/main/java/com/wjh/aicodegen/core/parser/HtmlCodeParser.java`
**行号**: 22-27

**现状代码**:
```java
String htmlCode = extractHtmlCode(codeContent);
if (htmlCode != null && !htmlCode.trim().isEmpty()) {
    result.setHtmlCode(htmlCode.trim());
} else {
    // 如果没有找到代码块，将整个内容作为HTML
    result.setHtmlCode(codeContent.trim());  // ← 不安全的 fallback
}
```

**问题**: 如果 AI 的回复包含解释文字 + 代码块，但正则没匹配到（比如 AI 用了 `` ```htm `` 而不是 `` ```html ``，或者代码块嵌套了 ```），fallback 会把整个 AI 回复（包括解释文字）当作 HTML 保存。

**修复方案**:
1. 增加更多 code fence 变体
2. fallback 时尝试提取 `<!DOCTYPE` 或 `<html` 开头的内容
3. 至少做一次 HTML 基础校验

**修复后代码参考**:
```java
private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
    "```(?:html|htm|xhtml)?\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

@Override
public HtmlCodeResult parseCode(String codeContent) {
    HtmlCodeResult result = new HtmlCodeResult();
    String htmlCode = extractHtmlCode(codeContent);
    if (htmlCode != null && !htmlCode.trim().isEmpty()) {
        result.setHtmlCode(htmlCode.trim());
    } else {
        // 尝试从整个内容中提取 HTML 部分
        String extracted = extractHtmlFromContent(codeContent);
        if (extracted != null) {
            result.setHtmlCode(extracted);
        } else {
            result.setHtmlCode(codeContent.trim());
        }
    }
    return result;
}

/**
 * 从内容中提取 HTML 部分（查找 <!DOCTYPE 或 <html 开始的内容）
 */
private String extractHtmlFromContent(String content) {
    // 尝试查找 HTML 文档的起始位置
    int docTypeIdx = content.indexOf("<!DOCTYPE");
    int htmlIdx = content.indexOf("<html");
    int startIdx = -1;
    if (docTypeIdx >= 0 && htmlIdx >= 0) {
        startIdx = Math.min(docTypeIdx, htmlIdx);
    } else if (docTypeIdx >= 0) {
        startIdx = docTypeIdx;
    } else if (htmlIdx >= 0) {
        startIdx = htmlIdx;
    }
    if (startIdx >= 0) {
        return content.substring(startIdx).trim();
    }
    return null;
}
```

`MultiFileCodeParser` 也需要同步增加 `` ```htm `` 等变体。

---

### ISSUE-006: 构建错误收集不充分

**文件**: `backend/src/main/java/com/wjh/aicodegen/core/builder/BuildRetryService.java`
**行号**: 99-135

**现状代码**:
```java
private String collectBuildErrors(String projectPath) {
    // 只读 .log 文件
    File[] logFiles = projectDir.listFiles((dir, name) ->
        name.endsWith(".log") || name.equals("npm-debug.log"));
    // ...
    if (errors.length() == 0) {
        errors.append("构建失败，可能是代码语法错误或依赖问题。");  // ← 无用的通用信息
    }
}
```

**问题**: `npm install` / `npm run build` 的错误通常输出到 stdout/stderr，不写日志文件。当没有 `.log` 文件时，返回的是无用的通用信息，AI 无法据此修复任何东西。

**修复方案**:
1. `VueProjectBuilder.executeCommand` 捕获进程的 stdout/stderr 输出并保存
2. `BuildRetryService` 读取这些输出
3. 至少捕获最后 N 行输出作为错误上下文

**涉及两个文件**:

**VueProjectBuilder.java** — 修改 `executeCommand` 捕获输出:
```java
private String lastBuildOutput = ""; // 新增字段

private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
    try {
        Process process = RuntimeUtil.exec(null, workingDir, command.split("\\s+"));
        // 捕获 stdout 和 stderr
        StringBuilder output = new StringBuilder();
        Thread stdoutReader = Thread.ofVirtual().start(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });
        Thread stderrReader = Thread.ofVirtual().start(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append("[stderr] ").append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        stdoutReader.join(5000);
        stderrReader.join(5000);
        lastBuildOutput = output.toString();
        if (!finished) {
            process.destroyForcibly();
            return false;
        }
        return process.exitValue() == 0;
    } catch (Exception e) {
        lastBuildOutput = "命令执行异常: " + e.getMessage();
        return false;
    }
}

public String getLastBuildOutput() {
    return lastBuildOutput;
}
```

**BuildRetryService.java** — 修改错误收集:
```java
private String collectBuildErrors(String projectPath) {
    StringBuilder errors = new StringBuilder();

    // 1. 读取日志文件（原有逻辑）
    // ...

    // 2. 读取构建输出（新增）
    if (errors.length() == 0) {
        // 尝试从 VueProjectBuilder 获取上次构建输出
        String buildOutput = vueProjectBuilder.getLastBuildOutput();
        if (buildOutput != null && !buildOutput.isBlank()) {
            // 取最后 100 行
            String[] lines = buildOutput.split("\n");
            int start = Math.max(0, lines.length - 100);
            errors.append("构建输出（最后 ").append(lines.length - start).append(" 行）:\n");
            for (int i = start; i < lines.length; i++) {
                errors.append(lines[i]).append("\n");
            }
        }
    }

    // 3. fallback
    if (errors.length() == 0) {
        errors.append("构建失败，无可用错误日志。项目目录结构: ");
        File[] files = new File(projectPath).listFiles();
        if (files != null) {
            for (File file : files) {
                errors.append(file.getName()).append(" ");
            }
        }
    }
    return errors.toString();
}
```

---

### ISSUE-007: VueProjectBuilder 没有消费进程输出流

**文件**: `backend/src/main/java/com/wjh/aicodegen/core/builder/VueProjectBuilder.java`
**行号**: 122-150

**现状代码**:
```java
Process process = RuntimeUtil.exec(null, workingDir, command.split("\\s+"));
boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
```

**问题**: 进程的 stdout/stderr 没有被消费。如果输出缓冲区满了，进程会 hang 住（尤其在 Windows 上）。虽然有 timeout 保护，但这会导致本可以成功的构建被误判为超时。

**修复方案**: 与 ISSUE-006 一起修复——在 `executeCommand` 中开线程消费 stdout/stderr。见 ISSUE-006 的 VueProjectBuilder 修复代码。

---

## P2 — 可以优化（改善可观测性和维护性）

### ISSUE-008: 两套审查系统并存

**文件**:
- `backend/src/main/java/com/wjh/aicodegen/core/AiCodeGeneratorFacade.java` (line 68)
- `backend/src/main/java/com/wjh/aicodegen/agent/AgentOrchestrator.java`
- `backend/src/main/java/com/wjh/aicodegen/langgraph4j/workflow/ReviewWorkflow.java`

**现状**:
```java
// AiCodeGeneratorFacade.java
@Resource
@Lazy
private AgentOrchestrator agentOrchestrator;  // 旧版，未使用但注入了
@Resource
private ReviewWorkflow reviewWorkflow;  // 新版，实际使用
```

**问题**: `AgentOrchestrator` 是旧版实现，`ReviewWorkflow` 是新版。两者 retry 逻辑、trace 记录方式不同。`AgentOrchestrator` 里的 `saveOptimizedCode()` 会重新解析和保存代码，可能与主流程冲突。

**修复方案**:
1. 确认 `AgentOrchestrator` 不再被任何地方调用
2. 从 `AiCodeGeneratorFacade` 中移除 `AgentOrchestrator` 的注入
3. 标记 `AgentOrchestrator` 为 `@Deprecated` 或直接删除

---

### ISSUE-009: 构建状态对用户不可见

**文件**: `backend/src/main/java/com/wjh/aicodegen/core/AiCodeGeneratorFacade.java`
**行号**: 241-242

**现状代码**:
```java
sink.complete();
triggerBuild(appId, buildStrategy);  // ← 流已关闭，用户不知道构建结果
```

**问题**: SSE 流在构建开始前就关闭了。用户看到"生成完成"，但构建可能失败。用户没有任何途径知道构建是否成功。

**修复方案**:
1. `triggerBuild` 完成后更新 app 的 `deployUrl` 或 `status` 字段
2. 或者在 SSE 流中延迟 complete，等待构建结果
3. 最简单的方案：在 `triggerBuild` 内部，构建完成后更新数据库中的 app 状态

---

### ISSUE-010: ReviewWorkflow 中 modelName 硬编码

**文件**: `backend/src/main/java/com/wjh/aicodegen/langgraph4j/workflow/ReviewWorkflow.java`
**行号**: 123, 162

**现状代码**:
```java
.modelName("deepseek-chat")  // ← 硬编码
```

**问题**: 本地环境用 `mimo-v2.5-pro`，生产用 `deepseek-chat`，但 trace 记录里硬编码了 `deepseek-chat`。监控数据不准确。

**修复方案**: 从配置中读取模型名称，或从 `MonitorContext` 中获取。

---

### ISSUE-011: collectGeneratedCode 50000 字符硬截断

**文件**: `backend/src/main/java/com/wjh/aicodegen/core/AiCodeGeneratorFacade.java`
**行号**: 411-413

**现状代码**:
```java
if (combinedCode.length() > 50000) {
    combinedCode = combinedCode.substring(0, 50000);  // ← 可能切断文件
}
```

**问题**: 硬截断可能把一个文件从中间切断，导致审查 AI 看到不完整的代码，给出错误的审查结论。

**修复方案**: 按文件边界截断——保留完整的文件，跳过超出限制的文件。

```java
private String collectGeneratedCode(Long appId) {
    File projectDir = findProjectDir(appId);
    if (projectDir == null || !projectDir.exists()) {
        return null;
    }
    List<String> codeFiles = collectCodeFiles(projectDir.toPath());
    if (codeFiles.isEmpty()) {
        return null;
    }
    // 按文件边界截断
    StringBuilder combined = new StringBuilder();
    int skippedFiles = 0;
    for (String fileContent : codeFiles) {
        if (combined.length() + fileContent.length() > 50000) {
            skippedFiles++;
            continue;
        }
        combined.append(fileContent).append("\n\n");
    }
    if (skippedFiles > 0) {
        log.info("代码审查截断: 跳过了 {} 个文件（总长度 {} 字符）",
                skippedFiles, combined.length());
    }
    return combined.toString();
}
```

---

### ISSUE-012: FileWriteTool 没有写入大小限制

**文件**: `backend/src/main/java/com/wjh/aicodegen/ai/tools/FileWriteTool.java`
**行号**: 47

**现状代码**:
```java
Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
```

**问题**: AI 可能写入超大文件（比如把整个 node_modules 内容误写入），没有大小限制。

**修复方案**: 增加单文件写入大小上限（建议 1MB）。

```java
private static final int MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1MB

public String writeFile(String relativeFilePath, String content, @ToolMemoryId Long appId) {
    // ...
    byte[] bytes = content.getBytes();
    if (bytes.length > MAX_FILE_SIZE_BYTES) {
        return "错误：文件大小超出限制（最大 1MB），实际大小: " + (bytes.length / 1024) + "KB";
    }
    Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    // ...
}
```

---

## 修复优先级建议

| 顺序 | Issue | 预计工作量 | 影响范围 |
|------|-------|-----------|---------|
| 1 | ISSUE-003 质检重试丢失 prompt | 5 分钟 | 质检重试生成质量 |
| 2 | ISSUE-001 质检异常静默放行 | 15 分钟 | 质检可靠性 |
| 3 | ISSUE-002 保存异常被吞掉 | 30 分钟 | 用户感知 |
| 4 | ISSUE-005 正则 fallback 不安全 | 15 分钟 | HTML 生成质量 |
| 5 | ISSUE-004 随机 appId 孤儿目录 | 20 分钟 | 磁盘空间 |
| 6 | ISSUE-006 + ISSUE-007 构建输出捕获 | 40 分钟 | 构建重试成功率 |
| 7 | ISSUE-008 清理旧审查系统 | 10 分钟 | 代码维护性 |
| 8 | ISSUE-009 构建状态可见性 | 30 分钟 | 用户体验 |
| 9 | ISSUE-010 modelName 硬编码 | 5 分钟 | 监控准确性 |
| 10 | ISSUE-011 截断优化 | 10 分钟 | 大项目审查质量 |
| 11 | ISSUE-012 写入大小限制 | 5 分钟 | 安全性 |

---

## 涉及文件清单

| 文件路径 | 涉及的 Issue |
|---------|-------------|
| `backend/src/main/java/com/wjh/aicodegen/core/AiCodeGeneratorFacade.java` | 002, 009, 011 |
| `backend/src/main/java/com/wjh/aicodegen/langgraph4j/node/CodeQualityCheckNode.java` | 001 |
| `backend/src/main/java/com/wjh/aicodegen/langgraph4j/node/CodeGeneratorNode.java` | 003, 004 |
| `backend/src/main/java/com/wjh/aicodegen/langgraph4j/CodeGenWorkflow.java` | 001 (重试上限) |
| `backend/src/main/java/com/wjh/aicodegen/core/parser/HtmlCodeParser.java` | 005 |
| `backend/src/main/java/com/wjh/aicodegen/core/parser/MultiFileCodeParser.java` | 005 |
| `backend/src/main/java/com/wjh/aicodegen/core/builder/VueProjectBuilder.java` | 006, 007 |
| `backend/src/main/java/com/wjh/aicodegen/core/builder/BuildRetryService.java` | 006 |
| `backend/src/main/java/com/wjh/aicodegen/agent/AgentOrchestrator.java` | 008 |
| `backend/src/main/java/com/wjh/aicodegen/langgraph4j/workflow/ReviewWorkflow.java` | 010 |
| `backend/src/main/java/com/wjh/aicodegen/ai/tools/FileWriteTool.java` | 012 |
| `backend/src/main/java/com/wjh/aicodegen/langgraph4j/state/WorkflowContext.java` | 004 (需增加 appId 字段) |
