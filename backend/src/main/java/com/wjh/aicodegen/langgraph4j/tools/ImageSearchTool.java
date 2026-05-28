package com.wjh.aicodegen.langgraph4j.tools;

import com.wjh.aicodegen.langgraph4j.model.ImageResource;
import com.wjh.aicodegen.langgraph4j.tools.imagesource.ParallelImageSearcher;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图片搜索工具（LangGraph4j 工作流版本）
 * 通过 ParallelImageSearcher 并行调用多个图片源（Pexels + Openverse）
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class ImageSearchTool {

    @Resource
    private ParallelImageSearcher parallelImageSearcher;

    @Tool("搜索内容相关的图片，用于网站内容展示。同时从多个图片源搜索，返回最佳结果。")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        return parallelImageSearcher.search(query, 4);
    }
}
