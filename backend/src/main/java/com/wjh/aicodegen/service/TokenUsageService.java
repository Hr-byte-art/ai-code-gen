package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.dto.token.GenerationStatsDTO;
import com.wjh.aicodegen.model.dto.token.ModelTokenRankingDTO;
import com.wjh.aicodegen.model.dto.token.ModelTokenSummaryDTO;
import com.wjh.aicodegen.model.dto.token.SystemTokenSummaryDTO;
import com.wjh.aicodegen.model.dto.token.TokenDetailDTO;
import com.wjh.aicodegen.model.dto.token.TokenRankingDTO;
import com.wjh.aicodegen.model.dto.token.UserTokenSummaryDTO;
import com.wjh.aicodegen.observability.model.entity.TokenUsage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Token 使用统计表 服务层。
 *
 * @author 王哈哈
 * @since 2025-09-23 21:18:48
 */
public interface TokenUsageService extends IService<TokenUsage> {

    /**
     * 根据appId和用途查找已存在的记录（用于Token累加）
     *
     * @param appId   应用ID
     * @param purpose AI调用用途
     * @return 已存在的记录，如果不存在返回null
     */
    TokenUsage findByAppIdAndPurpose(Long appId, String purpose);

    /**
     * 获取指定用户的Token消耗汇总
     *
     * @param userId 用户ID
     * @return 用户Token消耗汇总
     */
    UserTokenSummaryDTO getUserTokenSummary(Long userId);

    /**
     * 获取系统总Token消耗统计
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 系统Token消耗汇总
     */
    SystemTokenSummaryDTO getSystemTokenSummary(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户Token消耗排行榜
     *
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return Token消耗排行榜
     */
    TokenRankingDTO getTokenRanking(Integer page, Integer pageSize);

    /**
     * 获取指定用户的Token使用详情列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return Token使用详情列表
     */
    List<TokenDetailDTO> getUserTokenDetails(Long userId, Integer page, Integer pageSize);

    /**
     * 获取指定应用的Token使用详情列表
     *
     * @param appId    应用ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return Token使用详情列表
     */
    List<TokenDetailDTO> getAppTokenDetails(Long appId, Integer page, Integer pageSize);

    /**
     * 获取Token使用趋势数据（按天统计）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param userId    用户ID（可选，为null时统计全系统）
     * @return 每日Token消耗趋势
     */
    List<SystemTokenSummaryDTO> getTokenTrend(LocalDateTime startTime, LocalDateTime endTime, Long userId);

    /**
     * 获取指定模型的Token消耗汇总
     *
     * @param modelName 模型名称
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型Token消耗汇总
     */
    ModelTokenSummaryDTO getModelTokenSummary(String modelName, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取模型Token消耗排行榜
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param limit     返回数量限制（可选，默认不限制）
     * @return 模型Token消耗排行榜
     */
    ModelTokenRankingDTO getModelTokenRanking(LocalDateTime startTime, LocalDateTime endTime, Integer limit);

    /**
     * 获取所有模型的Token消耗统计列表
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 模型Token消耗统计列表
     */
    List<ModelTokenSummaryDTO> getAllModelTokenSummaries(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户生成历史统计
     *
     * @param userId 用户ID
     * @return 生成历史统计
     */
    GenerationStatsDTO getUserGenerationStats(Long userId);

}
