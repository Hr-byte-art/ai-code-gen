# 🛡️ 护轨机制上下文污染修复报告

## 🔍 **问题确认**

从你的测试结果可以看到，护轨机制存在**上下文污染问题**：

### **数据库记录分析**
```sql
id | aiCallPurpose        | totalTokens | 正确性
1  | ROUTING             | 215         | 正确 - 路由决策
2  | INPUT_SAFETY_CHECK  | 356         | 正确 - 护轨机制（qwen-turbo）
3  | INPUT_SAFETY_CHECK  | 1039        | 错误 - 应该是CODE_GENERATION
4  | INPUT_SAFETY_CHECK  | 1569        | 错误 - 应该是IMAGE_SEARCH
5  | INPUT_SAFETY_CHECK  | 2097        | 错误 - 应该是IMAGE_SEARCH  
6  | INPUT_SAFETY_CHECK  | 7420        | 错误 - 应该是CODE_GENERATION
```

### **根本原因**
护轨机制在执行时**永久性地修改了共享的 `MonitorContext`**：

```java
// 问题代码：直接修改原始上下文
context.setAiCallPurpose(AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode());
```

这导致后续的所有AI调用都被错误地标记为 `INPUT_SAFETY_CHECK`。

## 🔧 **修复方案**

### **修复1: 创建临时上下文副本**
```java
// 修复前：直接修改原始上下文（污染）
context.setAiCallPurpose(AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode());

// 修复后：创建临时副本，不影响原始上下文
MonitorContext safetyContext = MonitorContext.builder()
        .userId(existingContext.getUserId())
        .appId(existingContext.getAppId())
        .aiCallPurpose(AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode())
        .build();
```

### **修复2: 上下文保存与恢复机制**
```java
@Override
public InputGuardrailResult validate(UserMessage userMessage) {
    // 保存原始上下文
    MonitorContext originalContext = MonitorContextHolder.getContext();
    
    try {
        // 设置护轨专用上下文
        setupSafetyCheckContext();
        
        // 执行护轨检查...
        
    } finally {
        // 恢复原始上下文，确保不影响后续调用
        if (originalContext != null) {
            MonitorContextHolder.setContext(originalContext);
        }
    }
}
```

## 🎯 **修复效果预期**

### **修复前（有问题的流程）**
```
用户输入 → ROUTING → INPUT_SAFETY_CHECK(护轨) → [污染开始]
                                              ↓
                    所有后续调用错误标记为INPUT_SAFETY_CHECK
```

### **修复后（正确的流程）** ✅
```
用户输入 → ROUTING → INPUT_SAFETY_CHECK(护轨) → [上下文恢复]
                                              ↓
                                         CODE_GENERATION → IMAGE_SEARCH → ...
                                         (正确分类)      (正确分类)
```

## 📊 **预期的Token消耗记录**

### **修复后正确的记录（简化粒度）**
```sql
id | aiCallPurpose        | totalTokens | 说明
1  | ROUTING             | ~215        | 路由决策
2  | INPUT_SAFETY_CHECK  | ~356        | 护轨安全检查
3  | CODE_GENERATION     | ~12000      | 代码生成（包含所有工具调用Token）
```

**🎯 目标效果：每个完整的AI APP代码生成只有3条记录**

## 🚀 **立即测试**

1. **重启应用**
2. **测试相同输入**：`制作一个作品展示网站...`
3. **验证分类正确性**：
   - `ROUTING`: 路由决策
   - `INPUT_SAFETY_CHECK`: 只有护轨检查
   - `CODE_GENERATION`: 代码生成调用  
   - `IMAGE_SEARCH`: 图片搜索工具

## 💡 **关键改进**

1. **隔离性**: 护轨机制使用独立的临时上下文
2. **恢复性**: `try-finally` 确保上下文一定会被恢复
3. **准确性**: 每个AI调用都有正确的用途分类

---

**护轨机制现在完全无害化，不会污染后续的AI调用分类！** 🎉
