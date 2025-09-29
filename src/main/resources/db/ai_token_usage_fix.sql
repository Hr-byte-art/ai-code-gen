-- =====================================================
-- AI Token 使用统计表 - 完整修复脚本
-- 功能：修复AI调用用途字段，实现Token消耗统计
-- 作者：AI助手
-- 日期：2025-09-25
-- =====================================================

USE ai_code_gen;

-- 1. 检查当前表结构
SELECT '=== 当前表结构 ===' as info;
DESCRIBE token_usage_record;

-- 2. 检查aiCallPurpose字段是否存在
SET @ai_purpose_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'ai_code_gen' 
    AND TABLE_NAME = 'token_usage_record' 
    AND COLUMN_NAME = 'aiCallPurpose'
);

-- 3. 检查codeGenType字段是否存在
SET @codegen_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'ai_code_gen' 
    AND TABLE_NAME = 'token_usage_record' 
    AND COLUMN_NAME = 'codeGenType'
);

-- 4. 添加aiCallPurpose字段（如果不存在）
SELECT '=== 添加aiCallPurpose字段 ===' as info;
SET @add_column_sql = IF(@ai_purpose_exists = 0, 
    'ALTER TABLE token_usage_record ADD COLUMN aiCallPurpose VARCHAR(50) DEFAULT ''UNKNOWN'' COMMENT ''AI调用用途''',
    'SELECT ''aiCallPurpose字段已存在，跳过添加'' as message'
);
PREPARE add_stmt FROM @add_column_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

-- 5. 迁移codeGenType数据到aiCallPurpose（如果codeGenType存在）
SELECT '=== 迁移数据 ===' as info;
SET @migrate_sql = IF(@codegen_exists > 0, 
    'UPDATE token_usage_record SET aiCallPurpose = 
        CASE 
            WHEN codeGenType = ''ROUTING'' THEN ''ROUTING''
            WHEN codeGenType = ''html'' THEN ''CODE_GENERATION''
            WHEN codeGenType = ''MULTI_FILE'' THEN ''CODE_GENERATION''
            WHEN codeGenType = ''VUE_PROJECT'' THEN ''CODE_GENERATION''
            WHEN codeGenType = ''CHAT_INTERACTION'' THEN ''CHAT_INTERACTION''
            WHEN codeGenType = ''IMAGE_SEARCH'' THEN ''IMAGE_SEARCH''
            ELSE ''UNKNOWN''
        END
    WHERE aiCallPurpose IS NULL OR aiCallPurpose = ''UNKNOWN''',
    'SELECT ''codeGenType字段不存在，跳过数据迁移'' as message'
);
PREPARE migrate_stmt FROM @migrate_sql;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;

-- 6. 删除codeGenType字段（如果存在）
SELECT '=== 删除codeGenType字段 ===' as info;
SET @drop_column_sql = IF(@codegen_exists > 0, 
    'ALTER TABLE token_usage_record DROP COLUMN codeGenType',
    'SELECT ''codeGenType字段不存在，跳过删除'' as message'
);
PREPARE drop_stmt FROM @drop_column_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

-- 7. 查看修复后的表结构
SELECT '=== 修复后的表结构 ===' as info;
DESCRIBE token_usage_record;

-- 8. 查看最新的数据记录
SELECT '=== 最新数据记录 ===' as info;
SELECT 
    id, 
    userId, 
    appId, 
    modelName, 
    aiCallPurpose,
    inputTokens, 
    outputTokens, 
    totalTokens, 
    createTime
FROM token_usage_record 
ORDER BY createTime DESC 
LIMIT 10;

-- 9. 数据完整性验证
SELECT '=== 数据完整性验证 ===' as info;
SELECT 
    'Total Records' as metric,
    COUNT(*) as value
FROM token_usage_record
UNION ALL
SELECT 
    'Purpose Types' as metric,
    COUNT(DISTINCT aiCallPurpose) as value
FROM token_usage_record
UNION ALL
SELECT 
    'Null Purpose Count' as metric,
    COUNT(CASE WHEN aiCallPurpose IS NULL THEN 1 END) as value
FROM token_usage_record
UNION ALL
SELECT 
    'Unknown Purpose Count' as metric,
    COUNT(CASE WHEN aiCallPurpose = 'UNKNOWN' THEN 1 END) as value
FROM token_usage_record;

-- 10. 按AI调用用途统计Token消耗
SELECT '=== AI调用用途统计 ===' as info;
SELECT 
    COALESCE(aiCallPurpose, 'NULL_PURPOSE') as ai_call_purpose,
    COUNT(*) as call_count,
    SUM(totalTokens) as total_tokens,
    AVG(totalTokens) as avg_tokens,
    MIN(createTime) as first_call,
    MAX(createTime) as last_call
FROM token_usage_record 
GROUP BY aiCallPurpose
ORDER BY total_tokens DESC;

-- 11. 按用户统计Token消耗
SELECT '=== 用户Token消耗统计 ===' as info;
SELECT 
    userId,
    COUNT(*) as call_count,
    SUM(totalTokens) as total_tokens,
    AVG(totalTokens) as avg_tokens
FROM token_usage_record 
WHERE userId > 0
GROUP BY userId
ORDER BY total_tokens DESC
LIMIT 10;

-- 12. 按应用统计Token消耗
SELECT '=== 应用Token消耗统计 ===' as info;
SELECT 
    appId,
    COUNT(*) as call_count,
    SUM(totalTokens) as total_tokens,
    AVG(totalTokens) as avg_tokens
FROM token_usage_record 
WHERE appId > 0
GROUP BY appId
ORDER BY total_tokens DESC
LIMIT 10;

-- 13. 完成提示
SELECT '=== 修复完成 ===' as info;
SELECT 
    'AI调用用途字段修复完成！' as status,
    '现在可以重启应用程序测试功能' as next_step,
    'aiCallPurpose字段将存储：ROUTING, CODE_GENERATION, CHAT_INTERACTION, IMAGE_SEARCH等' as field_info;
