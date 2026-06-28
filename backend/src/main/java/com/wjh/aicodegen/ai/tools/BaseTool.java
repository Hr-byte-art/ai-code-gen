package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.core.saver.GeneratedProjectWorkspace;
import com.wjh.aicodegen.manager.SpringContextUtil;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.enums.AiCallPurposeEnum;
import com.wjh.aicodegen.monitor.GlobalContextStorage;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.service.AppService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具基类
 * 定义所有工具的通用接口
 * 
 * @author 王哈哈
 */
@Slf4j
public abstract class BaseTool {

    private static final Pattern WHOLE_FILE_MARKDOWN_FENCE = Pattern.compile(
            "\\A[\\s\\uFEFF]*```[\\w.+-]*\\s*\\R([\\s\\S]*?)\\R?```[\\s\\uFEFF]*\\z");

    /**
     * 清理模型误加的整文件 Markdown 代码围栏，避免 ```tsx 这类标记被写入真实源码。
     */
    protected String normalizeGeneratedFileContent(String relativeFilePath, String content) {
        if (content == null) {
            return "";
        }
        Matcher matcher = WHOLE_FILE_MARKDOWN_FENCE.matcher(content);
        if (!matcher.matches()) {
            return content;
        }
        String normalizedContent = matcher.group(1);
        log.info("清理整文件 Markdown 代码围栏: path={}, 原始长度={}, 清理后长度={}",
                relativeFilePath, content.length(), normalizedContent.length());
        return normalizedContent;
    }
    /**
     * 工具执行前设置MonitorContext
     * 用于解决工具调用时上下文丢失的问题
     * 
     * 重要：这个方法只是恢复上下文，不会永久修改aiCallPurpose
     * 具体的用途设置由AI调用时的listener处理
     */
    protected void setupMonitorContext() {
        // 尝试从多种方式获取MonitorContext
        MonitorContext context = MonitorContextHolder.getContext();

        if (context == null) {
            // 如果ThreadLocal中没有，尝试从全局存储中获取
            context = GlobalContextStorage.getLatestContext();
            if (context != null) {
                log.info("工具执行从全局存储中恢复MonitorContext: userId={}, appId={}, aiCallPurpose={}",
                        context.getUserId(), context.getAppId(), context.getAiCallPurpose());
                // 不修改aiCallPurpose，保持原有的用途
                MonitorContextHolder.setContext(context);
                log.info("工具执行恢复上下文成功: toolName={}, 保持原有aiCallPurpose={}",
                        getToolName(), context.getAiCallPurpose());
            } else {
                log.warn("工具执行时未找到MonitorContext，将使用默认上下文");
                // 创建一个默认的上下文，确保工具调用时也有统计信息
                context = MonitorContext.builder()
                        .userId("tool_user")
                        .appId("tool_app")
                        .aiCallPurpose(AiCallPurposeEnum.TOOL_EXECUTION.getCode())
                        .build();
                MonitorContextHolder.setContext(context);
            }
        } else {
            // 如果找到了，不需要修改，直接记录日志
            log.info("工具执行前MonitorContext已存在: userId={}, appId={}, toolName={}, aiCallPurpose={}",
                    context.getUserId(), context.getAppId(), getToolName(), context.getAiCallPurpose());
        }
    }

    /**
     * 根据 appId 解析项目目录名
     * 先检查磁盘上是否存在目录，再从数据库查询 codeGenType
     */
    protected String resolveProjectDirName(Long appId) {
        String[] candidates = {"landing_page_" + appId, "vue_project_" + appId, "fullstack_" + appId,
                "react_ts_" + appId, "nextjs_" + appId, "html_" + appId, "multi_file_" + appId};
        for (String dir : candidates) {
            if (GeneratedProjectWorkspace.stagingDir(dir).exists() || GeneratedProjectWorkspace.finalDir(dir).exists()) {
                return dir;
            }
        }
        // 磁盘上没有，从数据库查询
        try {
            AppService appService = SpringContextUtil.getBean(AppService.class);
            App app = appService.getById(appId);
            if (app != null && app.getCodeGenType() != null) {
                return app.getCodeGenType() + "_" + appId;
            }
        } catch (Exception e) {
            log.warn("查询应用 codeGenType 失败: appId={}", appId, e);
        }
        return "vue_project_" + appId;
    }

    protected Path resolveProjectRoot(Long appId) {
        return GeneratedProjectWorkspace.resolveReadableDir(resolveProjectDirName(appId)).toPath();
    }

    protected Path resolveWritableProjectRoot(Long appId) {
        return GeneratedProjectWorkspace.resolveWritableDir(resolveProjectDirName(appId)).toPath();
    }

    /**
     * 根据工具类型获取具体的AI调用用途
     * 子类可以重写此方法来提供更精确的分类
     * 
     * @return AI调用用途代码
     */
    protected String getToolSpecificPurpose() {
        String toolName = getToolName();

        // 根据工具名称映射到具体的AI调用用途
        switch (toolName) {
            // 图片和素材相关
            case "searchContentImages": return AiCallPurposeEnum.IMAGE_SEARCH.getCode();
            case "generateLogo": return AiCallPurposeEnum.LOGO_GENERATION.getCode();
            case "generateIllustration": return AiCallPurposeEnum.ILLUSTRATION_GENERATION.getCode();
            case "generateMermaidDiagram": return AiCallPurposeEnum.MERMAID_DIAGRAM.getCode();

            // 文件操作相关
            case "readFile": return AiCallPurposeEnum.FILE_READ.getCode();
            case "writeFile": return AiCallPurposeEnum.FILE_WRITE.getCode();
            case "modifyFile": return AiCallPurposeEnum.FILE_MODIFY.getCode();
            case "deleteFile": return AiCallPurposeEnum.FILE_DELETE.getCode();
            case "readDir":
            case "readDirectory": return AiCallPurposeEnum.DIRECTORY_READ.getCode();

            // 安全检查相关
            case "safetyCheck":
            case "inputSafetyCheck": return AiCallPurposeEnum.INPUT_SAFETY_CHECK.getCode();
            case "contentModeration": return AiCallPurposeEnum.CONTENT_MODERATION.getCode();

            // 代码相关
            case "codeQualityCheck": return AiCallPurposeEnum.CODE_QUALITY_CHECK.getCode();
            case "codeOptimization": return AiCallPurposeEnum.CODE_OPTIMIZATION.getCode();
            case "testGeneration": return AiCallPurposeEnum.TEST_GENERATION.getCode();

            // 项目分析相关
            case "projectAnalysis": return AiCallPurposeEnum.PROJECT_ANALYSIS.getCode();
            case "dependencyAnalysis": return AiCallPurposeEnum.DEPENDENCY_ANALYSIS.getCode();

            // 默认为通用工具执行
            default:
                log.debug("未找到工具 {} 的具体分类，使用默认分类", toolName);
                return AiCallPurposeEnum.TOOL_EXECUTION.getCode();
        }
    }

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n⏳ %s...\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);
}
