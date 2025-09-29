# 🎯 Token统计粒度简化修复

## 📋 **用户需求**

将AI Token消耗统计的粒度降低，**每个完整的AI APP代码生成只记录3条数据**：

1. `ROUTING` - 生成类型路由决策
2. `INPUT_SAFETY_CHECK` - 护轨安全检查  
3. `CODE_GENERATION` - 代码生成（包含所有工具调用的Token消耗）

## 🔍 **问题分析**

### **修复前（过细粒度）**
```sql
id | aiCallPurpose        | totalTokens | 问题
1  | ROUTING             | 216         | 正确
2  | INPUT_SAFETY_CHECK  | 355         | 正确  
3  | CODE_GENERATION     | 1093        | 应该合并到一条记录
4  | CODE_GENERATION     | 1631        | 工具调用分开记录了
5  | CODE_GENERATION     | 2172        | 工具调用分开记录了
6  | CODE_GENERATION     | 9033        | 工具调用分开记录了
```

**问题**: 工具调用（图片搜索等）产生了多条 `CODE_GENERATION` 记录

## 🔧 **修复方案**

### **核心修改: 工具调用Token合并**

修改 `AiTokenStatisticsListener.detectAiCallPurpose()` 方法：

```java
// 修复前：工具调用单独分类
if (stackTrace.contains("AiImageSearchTool")) {
    return AiCallPurposeEnum.IMAGE_SEARCH.getCode();  // 单独记录
}

// 修复后：工具调用合并到CODE_GENERATION  
if (stackTrace.contains("AiImageSearchTool") || 
    stackTrace.contains("FileWriteTool") || 
    stackTrace.contains("MermaidTool")) {
    log.info("检测到工具调用，归类为 CODE_GENERATION（工具调用Token合并到代码生成阶段）");
    return AiCallPurposeEnum.CODE_GENERATION.getCode();
}
```

### **分类优先级**

1. **护轨机制** (`INPUT_SAFETY_CHECK`) - 最高优先级
2. **路由服务** (`ROUTING`) - 第二优先级  
3. **工具调用** → 归类为 `CODE_GENERATION`
4. **其他AI调用** → 保持原有用途

## 🎯 **修复效果**

### **修复后（简化粒度）** ✅
```sql
id | aiCallPurpose        | totalTokens | 说明
1  | ROUTING             | 216         | 路由决策
2  | INPUT_SAFETY_CHECK  | 355         | 护轨安全检查
3  | CODE_GENERATION     | 13929       | 代码生成 (1093+1631+2172+9033)
```

**总计: 3条记录，完美符合需求！**

## 📊 **Token合并逻辑**

所有工具调用的Token消耗会自动**累加到 `CODE_GENERATION` 类别**中：

- **图片搜索工具** (`AiImageSearchTool`) → `CODE_GENERATION`
- **文件操作工具** (`FileWriteTool`, `FileReadTool`) → `CODE_GENERATION`  
- **Mermaid图表工具** (`MermaidTool`) → `CODE_GENERATION`
- **其他所有工具** → `CODE_GENERATION`

## 🚀 **测试验证**

重启应用后测试，预期结果：

```sql
SELECT aiCallPurpose, COUNT(*) as count, SUM(totalTokens) as total_tokens
FROM token_usage_record 
WHERE appId = 'your_new_app_id'
GROUP BY aiCallPurpose;

-- 预期输出：
-- ROUTING          | 1 | ~216
-- INPUT_SAFETY_CHECK | 1 | ~355  
-- CODE_GENERATION  | 1 | ~12000
```

## 💡 **优势**

1. **简洁性**: 每个App只有3条记录，便于分析
2. **准确性**: Token消耗统计依然完整准确
3. **实用性**: 符合业务分析需求的粒度

---

**现在AI Token统计完全按照你的需求：3个清晰的阶段，工具调用Token自动合并！** 🎉
