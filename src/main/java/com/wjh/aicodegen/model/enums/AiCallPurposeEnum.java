package com.wjh.aicodegen.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI调用用途枚举 - 用于详细记录每次AI调用的具体目的
 * 便于Token消耗分析和系统监控
 * 
 * @author AI Assistant
 */
@Getter
@AllArgsConstructor
public enum AiCallPurposeEnum {

    // ===== 路由和决策类 =====
    ROUTING("ROUTING", "代码生成类型路由", "AI决策选择HTML/MULTI_FILE/VUE_PROJECT"),
    APP_NAME_GENERATION("APP_NAME_GENERATION", "应用名称生成", "AI自动生成应用名称"),

    // ===== 代码生成类 =====
    CODE_GENERATION("CODE_GENERATION", "主代码生成", "AI生成HTML/CSS/JavaScript代码"),
    CODE_QUALITY_CHECK("CODE_QUALITY_CHECK", "代码质量检查", "AI检查生成代码的质量和完整性"),

    // ===== 图片和素材相关 =====
    IMAGE_SEARCH("IMAGE_SEARCH", "图片搜索", "AI搜索内容相关的图片素材"),
    LOGO_GENERATION("LOGO_GENERATION", "Logo生成", "AI生成项目Logo"),
    ILLUSTRATION_GENERATION("ILLUSTRATION_GENERATION", "插画生成", "AI生成插画素材"),

    // ===== 文件操作类 =====
    FILE_READ("FILE_READ", "文件读取", "AI读取项目文件内容"),
    FILE_WRITE("FILE_WRITE", "文件写入", "AI写入或创建文件"),
    FILE_MODIFY("FILE_MODIFY", "文件修改", "AI修改现有文件内容"),
    FILE_DELETE("FILE_DELETE", "文件删除", "AI删除文件"),
    DIRECTORY_READ("DIRECTORY_READ", "目录读取", "AI读取目录结构"),

    // ===== 图表和文档生成 =====
    MERMAID_DIAGRAM("MERMAID_DIAGRAM", "流程图生成", "AI生成Mermaid流程图"),
    DOCUMENTATION("DOCUMENTATION", "文档生成", "AI生成项目文档"),

    // ===== 安全和护轨 =====
    INPUT_SAFETY_CHECK("INPUT_SAFETY_CHECK", "输入安全检查", "AI检查用户输入内容是否安全"),
    OUTPUT_SAFETY_CHECK("OUTPUT_SAFETY_CHECK", "输出安全检查", "AI检查生成内容是否安全"),
    CONTENT_MODERATION("CONTENT_MODERATION", "内容审核", "AI进行内容合规性检查"),

    // ===== 交互和聊天 =====
    CHAT_INTERACTION("CHAT_INTERACTION", "聊天交互", "用户与AI的对话交互"),
    QUESTION_ANSWERING("QUESTION_ANSWERING", "问题回答", "AI回答用户问题"),

    // ===== 项目管理和分析 =====
    PROJECT_ANALYSIS("PROJECT_ANALYSIS", "项目分析", "AI分析项目结构和需求"),
    DEPENDENCY_ANALYSIS("DEPENDENCY_ANALYSIS", "依赖分析", "AI分析项目依赖关系"),

    // ===== 优化和重构 =====
    CODE_OPTIMIZATION("CODE_OPTIMIZATION", "代码优化", "AI优化代码性能和结构"),
    CODE_REFACTORING("CODE_REFACTORING", "代码重构", "AI重构代码结构"),

    // ===== 测试相关 =====
    TEST_GENERATION("TEST_GENERATION", "测试生成", "AI生成测试代码"),
    TEST_ANALYSIS("TEST_ANALYSIS", "测试分析", "AI分析测试结果"),

    // ===== 其他 =====
    UNKNOWN("UNKNOWN", "未知用途", "无法确定的AI调用用途"),
    TOOL_EXECUTION("TOOL_EXECUTION", "工具执行", "通用工具执行"),
    SYSTEM_OPERATION("SYSTEM_OPERATION", "系统操作", "系统级别的AI操作");

    /**
     * 用途代码
     */
    private final String code;

    /**
     * 用途名称
     */
    private final String name;

    /**
     * 用途描述
     */
    private final String description;

    /**
     * 根据代码获取枚举值
     * 
     * @param code 用途代码
     * @return 枚举值，如果找不到则返回UNKNOWN
     */
    public static AiCallPurposeEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }

        for (AiCallPurposeEnum purpose : values()) {
            if (purpose.getCode().equals(code.trim())) {
                return purpose;
            }
        }

        return UNKNOWN;
    }

    /**
     * 检查是否为代码生成相关的调用
     * 
     * @return true 如果是代码生成相关
     */
    public boolean isCodeRelated() {
        return this == CODE_GENERATION ||
                this == CODE_QUALITY_CHECK ||
                this == CODE_OPTIMIZATION ||
                this == CODE_REFACTORING ||
                this == TEST_GENERATION;
    }

    /**
     * 检查是否为文件操作相关的调用
     * 
     * @return true 如果是文件操作相关
     */
    public boolean isFileOperationRelated() {
        return this == FILE_READ ||
                this == FILE_WRITE ||
                this == FILE_MODIFY ||
                this == FILE_DELETE ||
                this == DIRECTORY_READ;
    }

    /**
     * 检查是否为安全检查相关的调用
     * 
     * @return true 如果是安全检查相关
     */
    public boolean isSafetyRelated() {
        return this == INPUT_SAFETY_CHECK ||
                this == OUTPUT_SAFETY_CHECK ||
                this == CONTENT_MODERATION;
    }

    /**
     * 检查是否为素材生成相关的调用
     * 
     * @return true 如果是素材生成相关
     */
    public boolean isAssetRelated() {
        return this == IMAGE_SEARCH ||
                this == LOGO_GENERATION ||
                this == ILLUSTRATION_GENERATION ||
                this == MERMAID_DIAGRAM;
    }
}