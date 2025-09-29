package com.wjh.aicodegen.model.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户Token消耗汇总DTO
 *
 * @author AI Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenSummaryDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 总输入Token数量
     */
    private Long totalInputTokens;

    /**
     * 总输出Token数量
     */
    private Long totalOutputTokens;

    /**
     * 总Token数量
     */
    private Long totalTokens;

    /**
     * 应用创建数量
     */
    private Long appCount;

    /**
     * 最近使用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 主要使用的模型
     */
    private String primaryModel;

    /**
     * 在排行榜中的排名
     */
    private Integer ranking;
}
