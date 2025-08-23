package com.wjh.aicodegen.langgraph4j.ai.factory;

import com.wjh.aicodegen.langgraph4j.ai.ImageCollectionService;
import com.wjh.aicodegen.langgraph4j.tools.IgoutuIllustrationTool;
import com.wjh.aicodegen.langgraph4j.tools.ImageSearchTool;
import com.wjh.aicodegen.langgraph4j.tools.LogoGeneratorTool;
import com.wjh.aicodegen.langgraph4j.tools.MermaidDiagramTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 木子宸
 */
@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private IgoutuIllustrationTool igoutuIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(
                        imageSearchTool,
                        igoutuIllustrationTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .build();
    }
}
