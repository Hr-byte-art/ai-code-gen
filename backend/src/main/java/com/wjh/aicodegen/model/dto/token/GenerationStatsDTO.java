package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户生成历史统计 DTO
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationStatsDTO {

    /**
     * 总生成次数
     */
    private Long totalGenerations;

    /**
     * 总 Token 消耗
     */
    private Long totalTokens;

    /**
     * HTML 类型应用数
     */
    private Long htmlCount;

    /**
     * 多文件类型应用数
     */
    private Long multiFileCount;

    /**
     * Vue 项目类型应用数
     */
    private Long vueCount;

    /**
     * 已部署应用数
     */
    private Long deployCount;

    /**
     * 最近生成记录
     */
    private List<RecentGeneration> recentRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentGeneration {
        private Long appId;
        private String appName;
        private String codeGenType;
        private Long tokenUsed;
        private String createTime;
    }
}
