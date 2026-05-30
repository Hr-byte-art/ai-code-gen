package com.wjh.aicodegen.langgraph4j.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
public class ImageCollectionPlan implements Serializable {

    /**
     * 内容图片搜索任务列表
     */
    private List<ImageSearchTask> contentImageTasks;

    /**
     * 插画图片搜索任务列表
     */
    private List<IllustrationTask> illustrationTasks;

    /**
     * 架构图生成任务列表
     */
    private List<DiagramTask> diagramTasks;

    /**
     * Logo生成任务列表
     */
    private List<LogoTask> logoTasks;

    /**
     * 内容图片搜索任务
     * 对应 ImageSearchTool.searchContentImages(String query)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageSearchTask implements Serializable {
        private String query;
    }

    /**
     * 插画图片搜索任务
     * 对应 UndrawIllustrationTool.searchIllustrations(String query)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IllustrationTask implements Serializable {
        private String query;
    }

    /**
     * 架构图生成任务
     * 对应 MermaidDiagramTool.generateMermaidDiagram(String mermaidCode, String description)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramTask implements Serializable {
        private String mermaidCode;
        private String description;
    }

    /**
     * Logo生成任务
     * 对应 LogoGeneratorTool.generateLogos(String description)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogoTask implements Serializable {
        private String description;
    }
}
