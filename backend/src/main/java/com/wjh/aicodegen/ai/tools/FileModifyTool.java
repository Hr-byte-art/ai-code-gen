package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import com.wjh.aicodegen.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件修改工具
 * 支持 AI 通过工具调用的方式修改文件内容
 * @author 王哈哈
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Tool("修改文件内容，用新内容替换指定的旧内容")
    public String modifyFile(@P("文件的相对路径") String relativeFilePath,
                             @P("要替换的旧内容") String oldContent,
                             @P("替换后的新内容") String newContent,
                             @ToolMemoryId Long appId) {
        try {
            Path path = Paths.get(relativeFilePath);
            Path projectRoot = resolveWritableProjectRoot(appId);
            if (!path.isAbsolute()) {
                path = projectRoot.resolve(relativeFilePath);
            }
            path = path.normalize();
            if (!path.startsWith(projectRoot.normalize())) {
                return "错误：路径超出项目目录，拒绝访问 - " + relativeFilePath;
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + relativeFilePath;
            }
            String normalizedOldContent = normalizeGeneratedFileContent(relativeFilePath, oldContent);
            String normalizedNewContent = normalizeGeneratedFileContent(relativeFilePath, newContent);
            String originalContent = Files.readString(path);
            if (!originalContent.contains(normalizedOldContent)) {
                return "警告：文件中未找到要替换的内容，文件未修改 - " + relativeFilePath;
            }
            String modifiedContent = originalContent.replace(normalizedOldContent, normalizedNewContent);
            if (originalContent.equals(modifiedContent)) {
                return "信息：替换后文件内容未发生变化 - " + relativeFilePath;
            }
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功修改文件: {}", path.toAbsolutePath());
            return "文件修改成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "修改文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("✅ %s → `%s`", getDisplayName(), relativeFilePath);
    }
}
