package com.wjh.aicodegen.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户配额展示 VO
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuotaVO {

    /**
     * 今日已生成次数
     */
    private Integer dailyGenUsed;

    /**
     * 每日生成次数上限
     */
    private Integer dailyGenLimit;

    /**
     * 本月已生成次数
     */
    private Integer monthlyGenUsed;

    /**
     * 每月生成次数上限
     */
    private Integer monthlyGenLimit;

    /**
     * 今日已消耗 Token
     */
    private Long dailyTokenUsed;

    /**
     * 每日 Token 消耗上限
     */
    private Long dailyTokenLimit;

    /**
     * 本月已消耗 Token
     */
    private Long monthlyTokenUsed;

    /**
     * 每月 Token 消耗上限
     */
    private Long monthlyTokenLimit;
}
