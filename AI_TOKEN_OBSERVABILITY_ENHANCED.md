# AI Token 可观测性 - 细粒度分类系统 🎯

## 📋 **概述**

本文档介绍了增强版的AI Token可观测性系统，实现了**细粒度的AI调用分类**，让你能够精确了解每个Token是如何被消耗的。

## 🔍 **新的AI调用用途分类**

### **1. 路由和决策类**
- `ROUTING` - 代码生成类型路由（选择HTML/MULTI_FILE/VUE_PROJECT）
- `APP_NAME_GENERATION` - AI自动生成应用名称

### **2. 代码生成类**
- `CODE_GENERATION` - 主代码生成（生成HTML/CSS/JavaScript代码）
- `CODE_QUALITY_CHECK` - 代码质量检查和验证
- `CODE_OPTIMIZATION` - 代码性能和结构优化
- `CODE_REFACTORING` - 代码重构
- `TEST_GENERATION` - 测试代码生成

### **3. 图片和素材相关**
- `IMAGE_SEARCH` - 图片搜索和素材收集
- `LOGO_GENERATION` - Logo设计和生成
- `ILLUSTRATION_GENERATION` - 插画和图标生成
- `MERMAID_DIAGRAM` - 流程图和架构图生成

### **4. 文件操作类**
- `FILE_READ` - 文件内容读取
- `FILE_WRITE` - 文件创建和写入
- `FILE_MODIFY` - 文件内容修改
- `FILE_DELETE` - 文件删除
- `DIRECTORY_READ` - 目录结构读取

### **5. 安全和护轨**
- `INPUT_SAFETY_CHECK` - 用户输入安全检查
- `OUTPUT_SAFETY_CHECK` - 生成内容安全检查
- `CONTENT_MODERATION` - 内容合规性审核

### **6. 交互和聊天**
- `CHAT_INTERACTION` - 用户与AI的对话交互
- `QUESTION_ANSWERING` - AI回答技术问题

### **7. 项目管理和分析**
- `PROJECT_ANALYSIS` - 项目结构和需求分析
- `DEPENDENCY_ANALYSIS` - 项目依赖关系分析

### **8. 其他分类**
- `DOCUMENTATION` - 项目文档生成
- `SYSTEM_OPERATION` - 系统级别操作
- `TOOL_EXECUTION` - 通用工具执行
- `UNKNOWN` - 无法确定的调用

## 🚀 **新功能特性**

### **1. 智能工具分类**
- 每个工具调用都会自动映射到对应的AI调用用途
- 图片搜索工具 → `IMAGE_SEARCH`
- 文件操作工具 → `FILE_READ/FILE_WRITE/FILE_MODIFY`
- 安全检查工具 → `INPUT_SAFETY_CHECK`

### **2. 护轨机制监控**
- 输入安全检查时会自动标记为 `INPUT_SAFETY_CHECK`
- 内容审核时会标记为 `CONTENT_MODERATION`
- 可以分析安全检查的Token消耗

### **3. 枚举驱动设计**
- 使用 `AiCallPurposeEnum` 枚举管理所有分类
- 类型安全，避免字符串错误
- 支持分类查询和过滤

## 📊 **数据库记录示例**

现在你可以看到更详细的Token使用记录：

```sql
SELECT userId, appId, modelName, aiCallPurpose, totalTokens, createTime 
FROM token_usage_record 
ORDER BY createTime DESC;
```

**预期输出：**
```
userId            | appId             | modelName    | aiCallPurpose        | totalTokens | createTime
317621940049289216| 328857987039092736| qwen-turbo   | ROUTING             | 215         | 2025-09-26 00:25:15
317621940049289216| 328857987039092736| deepseek-chat| CODE_GENERATION     | 1038        | 2025-09-26 00:25:20
317621940049289216| 328857987039092736| deepseek-chat| IMAGE_SEARCH        | 1559        | 2025-09-26 00:25:26
317621940049289216| 328857987039092736| deepseek-chat| IMAGE_SEARCH        | 2107        | 2025-09-26 00:25:31
317621940049289216| 328857987039092736| deepseek-chat| CODE_GENERATION     | 7340        | 2025-09-26 00:29:02
```

## 📈 **使用场景分析**

### **1. Token成本分析**
```sql
-- 分析每种用途的Token消耗
SELECT 
    aiCallPurpose,
    COUNT(*) as call_count,
    AVG(totalTokens) as avg_tokens,
    SUM(totalTokens) as total_tokens
FROM token_usage_record 
GROUP BY aiCallPurpose 
ORDER BY total_tokens DESC;
```

### **2. 用户行为分析**
```sql
-- 分析用户最常使用的AI功能
SELECT 
    userId,
    aiCallPurpose,
    COUNT(*) as usage_count,
    SUM(totalTokens) as total_consumption
FROM token_usage_record 
WHERE userId = 'your_user_id'
GROUP BY userId, aiCallPurpose
ORDER BY total_consumption DESC;
```

### **3. 模型效率对比**
```sql
-- 对比不同模型在各种任务上的Token效率
SELECT 
    modelName,
    aiCallPurpose,
    AVG(totalTokens) as avg_tokens_per_call,
    COUNT(*) as call_count
FROM token_usage_record 
GROUP BY modelName, aiCallPurpose
ORDER BY modelName, avg_tokens_per_call DESC;
```

## 🔧 **技术实现**

### **1. 上下文传递机制**
- **GlobalContextStorage**: 全局上下文存储，解决多线程上下文丢失
- **BaseTool.setupMonitorContext()**: 工具执行前自动设置上下文
- **RequestContextManager**: 基于请求ID的上下文管理

### **2. 细粒度分类逻辑**
```java
// 工具自动分类
protected String getToolSpecificPurpose() {
    String toolName = getToolName();
    return switch (toolName) {
        case "searchContentImages" -> AiCallPurposeEnum.IMAGE_SEARCH.getCode();
        case "writeFile" -> AiCallPurposeEnum.FILE_WRITE.getCode();
        case "readFile" -> AiCallPurposeEnum.FILE_READ.getCode();
        // ... 更多映射
        default -> AiCallPurposeEnum.TOOL_EXECUTION.getCode();
    };
}
```

### **3. 护轨机制集成**
```java
// 安全检查时自动设置上下文
private void setupSafetyCheckContext() {
    MonitorContext context = MonitorContextHolder.getContext();
    if (context != null) {
        context.setAiCallPurpose(AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode());
        MonitorContextHolder.setContext(context);
    }
}
```

## 🎯 **核心优势**

1. **📊 精确统计**: 每种AI调用都有明确的用途标识
2. **💰 成本透明**: 可以分析哪种功能消耗Token最多
3. **🔍 行为洞察**: 了解用户最常使用的AI功能
4. **⚡ 性能优化**: 识别Token效率较低的操作
5. **🛡️ 安全监控**: 独立监控安全检查的Token消耗
6. **📈 趋势分析**: 跟踪不同功能的使用趋势

## 🚀 **下一步扩展**

- **实时监控面板**: 显示各种用途的Token消耗
- **智能预警**: Token消耗异常时的自动告警
- **成本预测**: 基于历史数据预测Token成本
- **用户画像**: 基于AI使用模式的用户分析

---

**恭喜！** 🎉 你现在拥有了业界最详细的AI Token可观测性系统！每一个Token的去向都清晰可见！
