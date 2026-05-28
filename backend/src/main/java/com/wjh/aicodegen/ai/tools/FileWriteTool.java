package com.wjh.aicodegen.ai.tools;

import cn.hutool.core.io.FileUtil;
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
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 * @author 王哈哈
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @Tool("写入文件到指定路径")
    public String writeFile(@P("文件的相对路径") String relativeFilePath, @P("要写入文件的内容") String content, @ToolMemoryId Long appId  ) {
        try {
            Path path = Paths.get(relativeFilePath);
            Path projectRoot = resolveProjectRoot(appId);
            if (!path.isAbsolute()) {
                // 相对路径处理，创建基于 appId 的项目目录
                path = projectRoot.resolve(relativeFilePath);
            }
            // 路径遍历校验：规范化后必须在项目目录内
            path = path.normalize();
            if (!path.startsWith(projectRoot.normalize())) {
                return "错误：路径超出项目目录，拒绝访问 - " + relativeFilePath;
            }
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {} (大小: {} 字节)", path.toAbsolutePath(), content.getBytes().length);
            
            // 如果是package.json文件，额外记录日志
            if (relativeFilePath.equals("package.json")) {
                log.info("Vue项目核心文件package.json已写入，项目目录: {}", path.getParent());
            }
            
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("✅ %s → `%s`", getDisplayName(), relativeFilePath);
    }
}
