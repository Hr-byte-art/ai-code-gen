package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型Token消耗排行榜DTO
 *
 * @author AI Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelTokenRankingDTO {

    /**
     * 模型排行榜列表
     */
    private List<ModelTokenSummaryDTO> rankings;

    /**
     * 总模型数
     */
    private Integer totalModels;

    /**
     * 统计开始时间
     */
    private LocalDateTime statisticsStartTime;

    /**
     * 统计结束时间
     */
    private LocalDateTime statisticsEndTime;

    /**
     * 总Token数量
     */
    private Long totalTokens;

    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 排行榜类型
     */
    private String rankingType;

    /**
     * 最活跃的模型
     */
    private String mostActiveModel;

    /**
     * 最高效的模型（每Token最低成本）
     */
    private String mostEfficientModel;
}
