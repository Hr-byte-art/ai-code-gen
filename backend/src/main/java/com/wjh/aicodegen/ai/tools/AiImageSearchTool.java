package com.wjh.aicodegen.ai.tools;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONObject;
import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.tools.imagesource.ParallelImageSearcher;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图片搜索工具（AI 代码生成版本）
 * 通过 ParallelImageSearcher 并行调用多个图片源（Pexels + Openverse）
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class AiImageSearchTool extends BaseTool {

    @Resource
    private ParallelImageSearcher parallelImageSearcher;

    @Tool("搜索内容相关的图片，用于网站内容展示。同时从多个图片源搜索，返回最佳结果。")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        setupMonitorContext();

        if (query == null || query.isBlank()) {
            log.warn("图片搜索关键词为空，返回空列表");
            return ListUtil.empty();
        }

        return parallelImageSearcher.search(query.trim(), 6);
    }

    @Override
    public String getToolName() {
        return "searchContentImages";
    }

    @Override
    public String getDisplayName() {
        return "搜索内容相关的图片";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("✅ %s", getDisplayName());
    }
}
