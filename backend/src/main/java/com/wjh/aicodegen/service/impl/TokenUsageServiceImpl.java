package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.TokenUsageMapper;
import com.wjh.aicodegen.model.dto.token.GenerationStatsDTO;
import com.wjh.aicodegen.model.dto.token.ModelTokenRankingDTO;
import com.wjh.aicodegen.model.dto.token.ModelTokenSummaryDTO;
import com.wjh.aicodegen.model.dto.token.SystemTokenSummaryDTO;
import com.wjh.aicodegen.model.dto.token.TokenDetailDTO;
import com.wjh.aicodegen.model.dto.token.TokenRankingDTO;
import com.wjh.aicodegen.model.dto.token.UserTokenSummaryDTO;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.observability.model.entity.TokenUsage;
import com.wjh.aicodegen.service.AppService;
import com.wjh.aicodegen.service.TokenUsageService;
import com.wjh.aicodegen.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * AI Token 使用统计表 服务层实现。
 *
 * @author 王哈哈
 * @since 2025-09-23 21:18:48
 */
@Slf4j
@Service("tokenUsageServiceImpl")
public class TokenUsageServiceImpl extends ServiceImpl<TokenUsageMapper, TokenUsage> implements TokenUsageService {

        @Resource
        private UserService userService;

        @Resource
        private AppService appService;

        @Override
        public TokenUsage findByAppIdAndPurpose(Long appId, String purpose) {
                return getOne(QueryWrapper.create()
                                .where("appId = ? AND aiCallPurpose = ?", appId, purpose));
        }

        @Override
        public UserTokenSummaryDTO getUserTokenSummary(Long userId) {
                try {
                        // 获取用户信息
                        User user = userService.getById(userId);
                        if (user == null) {
                                log.warn("用户不存在: userId={}", userId);
                                return null;
                        }

                        // 查询用户的Token统计
                        List<TokenUsage> userTokens = list(QueryWrapper.create()
                                        .where("userId = ?", userId));

                        if (userTokens.isEmpty()) {
                                return UserTokenSummaryDTO.builder()
                                                .userId(userId)
                                                .userName(user.getUserName())
                                                .userAvatar(user.getUserAvatar())
                                                .totalInputTokens(0L)
                                                .totalOutputTokens(0L)
                                                .totalTokens(0L)
                                                .appCount(0L)
                                                .ranking(0)
                                                .build();
                        }

                        // 计算汇总数据
                        long totalInput = userTokens.stream()
                                        .mapToLong(t -> t.getRequestTokenCount() != null ? t.getRequestTokenCount() : 0)
                                        .sum();
                        long totalOutput = userTokens.stream()
                                        .mapToLong(t -> t.getResponseTokenCount() != null ? t.getResponseTokenCount()
                                                        : 0)
                                        .sum();
                        long totalTokens = userTokens.stream()
                                        .mapToLong(t -> t.getTotalTokenCount() != null ? t.getTotalTokenCount() : 0)
                                        .sum();

                        // 获取应用数量
                        long appCount = userTokens.stream()
                                        .map(TokenUsage::getAppId)
                                        .distinct()
                                        .count();

                        // 获取最近使用时间
                        LocalDateTime lastUsed = userTokens.stream()
                                        .map(TokenUsage::getCreateTime)
                                        .filter(Objects::nonNull)
                                        .max(LocalDateTime::compareTo)
                                        .orElse(null);

                        // 获取主要使用的模型
                        String primaryModel = userTokens.stream()
                                        .collect(Collectors.groupingBy(TokenUsage::getModelName,
                                                        Collectors.summingLong(t -> t.getTotalTokenCount() != null
                                                                        ? t.getTotalTokenCount()
                                                                        : 0)))
                                        .entrySet().stream()
                                        .max(Map.Entry.comparingByValue())
                                        .map(Map.Entry::getKey)
                                        .orElse("未知");

                        return UserTokenSummaryDTO.builder()
                                        .userId(userId)
                                        .userName(user.getUserName())
                                        .userAvatar(user.getUserAvatar())
                                        .totalInputTokens(totalInput)
                                        .totalOutputTokens(totalOutput)
                                        .totalTokens(totalTokens)
                                        .appCount(appCount)
                                        .lastUsedTime(lastUsed)
                                        .primaryModel(primaryModel)
                                        .build();

                } catch (Exception e) {
                        log.error("获取用户Token汇总失败: userId={}, error={}", userId, e.getMessage(), e);
                        return null;
                }
        }

        @Override
        public SystemTokenSummaryDTO getSystemTokenSummary(LocalDateTime startTime, LocalDateTime endTime) {
                try {
                        QueryWrapper queryWrapper = QueryWrapper.create();
                        if (startTime != null) {
                                queryWrapper.and("createTime >= ?", startTime);
                        }
                        if (endTime != null) {
                                queryWrapper.and("createTime <= ?", endTime);
                        }

                        List<TokenUsage> allTokens = list(queryWrapper);

                        if (allTokens.isEmpty()) {
                                return SystemTokenSummaryDTO.builder()
                                                .totalInputTokens(0L)
                                                .totalOutputTokens(0L)
                                                .totalTokens(0L)
                                                .totalUsers(0L)
                                                .totalApps(0L)
                                                .totalCalls(0L)
                                                .tokenByPurpose(new HashMap<>())
                                                .tokenByModel(new HashMap<>())
                                                .statisticsStartTime(startTime)
                                                .statisticsEndTime(endTime)
                                                .avgTokenPerUser(0.0)
                                                .avgTokenPerApp(0.0)
                                                .build();
                        }

                        // 计算总数统计
                        long totalInput = allTokens.stream()
                                        .mapToLong(t -> t.getRequestTokenCount() != null ? t.getRequestTokenCount() : 0)
                                        .sum();
                        long totalOutput = allTokens.stream()
                                        .mapToLong(t -> t.getResponseTokenCount() != null ? t.getResponseTokenCount()
                                                        : 0)
                                        .sum();
                        long totalTokens = allTokens.stream()
                                        .mapToLong(t -> t.getTotalTokenCount() != null ? t.getTotalTokenCount() : 0)
                                        .sum();

                        // 统计用户、应用、调用数
                        long totalUsers = allTokens.stream().map(TokenUsage::getUserId).distinct().count();
                        long totalApps = allTokens.stream().map(TokenUsage::getAppId).distinct().count();
                        long totalCalls = allTokens.size();

                        // 按用途分组统计
                        Map<String, Long> tokenByPurpose = allTokens.stream()
                                        .collect(Collectors.groupingBy(
                                                        t -> t.getAppType() != null ? t.getAppType() : "UNKNOWN",
                                                        Collectors.summingLong(t -> t.getTotalTokenCount() != null
                                                                        ? t.getTotalTokenCount()
                                                                        : 0)));

                        // 按模型分组统计
                        Map<String, Long> tokenByModel = allTokens.stream()
                                        .collect(Collectors.groupingBy(
                                                        t -> t.getModelName() != null ? t.getModelName() : "未知",
                                                        Collectors.summingLong(t -> t.getTotalTokenCount() != null
                                                                        ? t.getTotalTokenCount()
                                                                        : 0)));

                        // 计算平均值
                        double avgTokenPerUser = totalUsers > 0 ? (double) totalTokens / totalUsers : 0.0;
                        double avgTokenPerApp = totalApps > 0 ? (double) totalTokens / totalApps : 0.0;

                        return SystemTokenSummaryDTO.builder()
                                        .totalInputTokens(totalInput)
                                        .totalOutputTokens(totalOutput)
                                        .totalTokens(totalTokens)
                                        .totalUsers(totalUsers)
                                        .totalApps(totalApps)
                                        .totalCalls(totalCalls)
                                        .tokenByPurpose(tokenByPurpose)
                                        .tokenByModel(tokenByModel)
                                        .statisticsStartTime(startTime)
                                        .statisticsEndTime(endTime)
                                        .avgTokenPerUser(avgTokenPerUser)
                                        .avgTokenPerApp(avgTokenPerApp)
                                        .build();

                } catch (Exception e) {
                        log.error("获取系统Token汇总失败: error={}", e.getMessage(), e);
                        return null;
                }
        }

        @Override
        public TokenRankingDTO getTokenRanking(Integer page, Integer pageSize) {
                try {
                        // 设置默认值
                        page = page != null && page > 0 ? page : 1;
                        pageSize = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 20;

                        // 获取所有用户的Token统计
                        List<TokenUsage> allTokens = list();

                        // 按用户ID分组并计算汇总
                        Map<Long, UserTokenSummaryDTO> userSummaryMap = new HashMap<>();

                        for (TokenUsage token : allTokens) {
                                if (token.getUserId() == null)
                                        continue;

                                UserTokenSummaryDTO summary = userSummaryMap.computeIfAbsent(token.getUserId(),
                                                userId -> {
                                                        User user = userService.getById(userId);
                                                        return UserTokenSummaryDTO.builder()
                                                                        .userId(userId)
                                                                        .userName(user != null ? user.getUserName()
                                                                                        : "未知用户")
                                                                        .userAvatar(user != null ? user.getUserAvatar()
                                                                                        : null)
                                                                        .totalInputTokens(0L)
                                                                        .totalOutputTokens(0L)
                                                                        .totalTokens(0L)
                                                                        .appCount(0L)
                                                                        .build();
                                                });

                                // 累加Token数量
                                summary.setTotalInputTokens(summary.getTotalInputTokens() +
                                                (token.getRequestTokenCount() != null ? token.getRequestTokenCount()
                                                                : 0));
                                summary.setTotalOutputTokens(summary.getTotalOutputTokens() +
                                                (token.getResponseTokenCount() != null ? token.getResponseTokenCount()
                                                                : 0));
                                summary.setTotalTokens(summary.getTotalTokens() +
                                                (token.getTotalTokenCount() != null ? token.getTotalTokenCount() : 0));
                        }

                        // 按用户统计应用数量
                        Map<Long, Long> appCountMap = allTokens.stream()
                                        .filter(t -> t.getUserId() != null && t.getAppId() != null)
                                        .collect(Collectors.groupingBy(TokenUsage::getUserId,
                                                        Collectors.mapping(TokenUsage::getAppId,
                                                                        Collectors.collectingAndThen(Collectors.toSet(),
                                                                                        Set::size))))
                                        .entrySet().stream()
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue()));

                        // 设置应用数量
                        userSummaryMap.forEach(
                                        (userId, summary) -> summary.setAppCount(appCountMap.getOrDefault(userId, 0L)));

                        // 按总Token数量排序
                        List<UserTokenSummaryDTO> sortedList = userSummaryMap.values().stream()
                                        .sorted((a, b) -> Long.compare(b.getTotalTokens(), a.getTotalTokens()))
                                        .collect(Collectors.toList());

                        // 设置排名
                        for (int i = 0; i < sortedList.size(); i++) {
                                sortedList.get(i).setRanking(i + 1);
                        }

                        // 分页处理
                        int totalUsers = sortedList.size();
                        int totalPages = (int) Math.ceil((double) totalUsers / pageSize);
                        int startIndex = (page - 1) * pageSize;
                        int endIndex = Math.min(startIndex + pageSize, totalUsers);

                        List<UserTokenSummaryDTO> pagedList = startIndex < totalUsers
                                        ? sortedList.subList(startIndex, endIndex)
                                        : new ArrayList<>();

                        return TokenRankingDTO.builder()
                                        .rankings(pagedList)
                                        .totalUsers((long) totalUsers)
                                        .currentPage(page)
                                        .pageSize(pageSize)
                                        .totalPages(totalPages)
                                        .rankingType("总Token消耗排行榜")
                                        .build();

                } catch (Exception e) {
                        log.error("获取Token排行榜失败: error={}", e.getMessage(), e);
                        return null;
                }
        }

        @Override
        public List<TokenDetailDTO> getUserTokenDetails(Long userId, Integer page, Integer pageSize) {
                try {
                        page = page != null && page > 0 ? page : 1;
                        pageSize = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 20;

                        Page<TokenUsage> tokenPage = page(new Page<>(page, pageSize),
                                        QueryWrapper.create()
                                                        .where("userId = ?", userId)
                                                        .orderBy("createTime", false));

                        return tokenPage.getRecords().stream()
                                        .map(this::convertToDetailDTO)
                                        .collect(Collectors.toList());

                } catch (Exception e) {
                        log.error("获取用户Token详情失败: userId={}, error={}", userId, e.getMessage(), e);
                        return new ArrayList<>();
                }
        }

        @Override
        public List<TokenDetailDTO> getAppTokenDetails(Long appId, Integer page, Integer pageSize) {
                try {
                        page = page != null && page > 0 ? page : 1;
                        pageSize = pageSize != null && pageSize > 0 ? Math.min(pageSize, 100) : 20;

                        Page<TokenUsage> tokenPage = page(new Page<>(page, pageSize),
                                        QueryWrapper.create()
                                                        .where("appId = ?", appId)
                                                        .orderBy("createTime", false));

                        return tokenPage.getRecords().stream()
                                        .map(this::convertToDetailDTO)
                                        .collect(Collectors.toList());

                } catch (Exception e) {
                        log.error("获取应用Token详情失败: appId={}, error={}", appId, e.getMessage(), e);
                        return new ArrayList<>();
                }
        }

        @Override
        public List<SystemTokenSummaryDTO> getTokenTrend(LocalDateTime startTime, LocalDateTime endTime, Long userId) {
                // 这里实现按天统计的趋势数据，暂时返回空列表
                // 可以根据需要实现按日期分组的统计逻辑
                log.info("获取Token使用趋势: startTime={}, endTime={}, userId={}", startTime, endTime, userId);
                return new ArrayList<>();
        }

        /**
         * 转换TokenUsage为TokenDetailDTO
         */
        private TokenDetailDTO convertToDetailDTO(TokenUsage tokenUsage) {
                // 获取用户信息
                User user = userService.getById(tokenUsage.getUserId());
                String userName = user != null ? user.getUserName() : "未知用户";

                // 获取应用信息
                App app = appService.getById(tokenUsage.getAppId());
                String appName = app != null ? app.getAppName() : "未知应用";

                // 获取用途描述
                String purposeDescription = getPurposeDescription(tokenUsage.getAppType());

                return TokenDetailDTO.builder()
                                .id(tokenUsage.getId())
                                .userId(tokenUsage.getUserId())
                                .userName(userName)
                                .appId(tokenUsage.getAppId())
                                .appName(appName)
                                .modelName(tokenUsage.getModelName())
                                .aiCallPurpose(tokenUsage.getAppType())
                                .inputTokens(tokenUsage.getRequestTokenCount())
                                .outputTokens(tokenUsage.getResponseTokenCount())
                                .totalTokens(tokenUsage.getTotalTokenCount())
                                .createTime(tokenUsage.getCreateTime())
                                .purposeDescription(purposeDescription)
                                .build();
        }

        @Override
        public ModelTokenSummaryDTO getModelTokenSummary(String modelName, LocalDateTime startTime,
                        LocalDateTime endTime) {
                try {
                        QueryWrapper queryWrapper = QueryWrapper.create()
                                        .where("modelName = ?", modelName);

                        if (startTime != null) {
                                queryWrapper.and("createTime >= ?", startTime);
                        }
                        if (endTime != null) {
                                queryWrapper.and("createTime <= ?", endTime);
                        }

                        List<TokenUsage> modelTokens = list(queryWrapper);

                        if (modelTokens.isEmpty()) {
                                return ModelTokenSummaryDTO.builder()
                                                .modelName(modelName)
                                                .modelDisplayName(getModelDisplayName(modelName))
                                                .totalInputTokens(0L)
                                                .totalOutputTokens(0L)
                                                .totalTokens(0L)
                                                .callCount(0L)
                                                .userCount(0L)
                                                .appCount(0L)
                                                .avgTokensPerCall(0.0)
                                                .avgTokensPerUser(0.0)
                                                .percentage(0.0)
                                                .modelProvider(getModelProvider(modelName))
                                                .estimatedCost(0.0)
                                                .build();
                        }

                        // 计算统计数据
                        long totalInput = modelTokens.stream()
                                        .mapToLong(t -> t.getRequestTokenCount() != null ? t.getRequestTokenCount() : 0)
                                        .sum();
                        long totalOutput = modelTokens.stream()
                                        .mapToLong(t -> t.getResponseTokenCount() != null ? t.getResponseTokenCount()
                                                        : 0)
                                        .sum();
                        long totalTokens = modelTokens.stream()
                                        .mapToLong(t -> t.getTotalTokenCount() != null ? t.getTotalTokenCount() : 0)
                                        .sum();

                        long callCount = modelTokens.size();
                        long userCount = modelTokens.stream().map(TokenUsage::getUserId).distinct().count();
                        long appCount = modelTokens.stream().map(TokenUsage::getAppId).distinct().count();

                        double avgTokensPerCall = callCount > 0 ? (double) totalTokens / callCount : 0.0;
                        double avgTokensPerUser = userCount > 0 ? (double) totalTokens / userCount : 0.0;

                        // 获取时间范围
                        LocalDateTime firstUsed = modelTokens.stream()
                                        .map(TokenUsage::getCreateTime)
                                        .filter(Objects::nonNull)
                                        .min(LocalDateTime::compareTo)
                                        .orElse(null);
                        LocalDateTime lastUsed = modelTokens.stream()
                                        .map(TokenUsage::getCreateTime)
                                        .filter(Objects::nonNull)
                                        .max(LocalDateTime::compareTo)
                                        .orElse(null);

                        // 生成用途分布
                        String purposeDistribution = generatePurposeDistribution(modelTokens);

                        // 估算成本
                        double estimatedCost = calculateEstimatedCost(modelName, totalInput, totalOutput);

                        return ModelTokenSummaryDTO.builder()
                                        .modelName(modelName)
                                        .modelDisplayName(getModelDisplayName(modelName))
                                        .totalInputTokens(totalInput)
                                        .totalOutputTokens(totalOutput)
                                        .totalTokens(totalTokens)
                                        .callCount(callCount)
                                        .userCount(userCount)
                                        .appCount(appCount)
                                        .avgTokensPerCall(avgTokensPerCall)
                                        .avgTokensPerUser(avgTokensPerUser)
                                        .firstUsedTime(firstUsed)
                                        .lastUsedTime(lastUsed)
                                        .purposeDistribution(purposeDistribution)
                                        .modelProvider(getModelProvider(modelName))
                                        .estimatedCost(estimatedCost)
                                        .build();

                } catch (Exception e) {
                        log.error("获取模型Token汇总失败: modelName={}, error={}", modelName, e.getMessage(), e);
                        return null;
                }
        }

        @Override
        public ModelTokenRankingDTO getModelTokenRanking(LocalDateTime startTime, LocalDateTime endTime,
                        Integer limit) {
                try {
                        List<ModelTokenSummaryDTO> allModels = getAllModelTokenSummaries(startTime, endTime);

                        // 按总Token数量排序
                        List<ModelTokenSummaryDTO> sortedModels = allModels.stream()
                                        .sorted((a, b) -> Long.compare(b.getTotalTokens(), a.getTotalTokens()))
                                        .collect(Collectors.toList());

                        // 设置排名和百分比
                        long totalTokensAll = sortedModels.stream().mapToLong(ModelTokenSummaryDTO::getTotalTokens)
                                        .sum();
                        long totalCallsAll = sortedModels.stream().mapToLong(ModelTokenSummaryDTO::getCallCount).sum();

                        for (int i = 0; i < sortedModels.size(); i++) {
                                ModelTokenSummaryDTO model = sortedModels.get(i);
                                model.setRanking(i + 1);
                                if (totalTokensAll > 0) {
                                        model.setPercentage((double) model.getTotalTokens() / totalTokensAll * 100);
                                }
                        }

                        // 应用数量限制
                        if (limit != null && limit > 0) {
                                sortedModels = sortedModels.stream().limit(limit).collect(Collectors.toList());
                        }

                        // 找到最活跃和最高效的模型
                        String mostActiveModel = sortedModels.stream()
                                        .max(Comparator.comparing(ModelTokenSummaryDTO::getCallCount))
                                        .map(ModelTokenSummaryDTO::getModelName)
                                        .orElse("未知");

                        String mostEfficientModel = sortedModels.stream()
                                        .filter(m -> m.getEstimatedCost() != null && m.getTotalTokens() > 0)
                                        .min(Comparator.comparing(m -> m.getEstimatedCost() / m.getTotalTokens()))
                                        .map(ModelTokenSummaryDTO::getModelName)
                                        .orElse("未知");

                        return ModelTokenRankingDTO.builder()
                                        .rankings(sortedModels)
                                        .totalModels(allModels.size())
                                        .statisticsStartTime(startTime)
                                        .statisticsEndTime(endTime)
                                        .totalTokens(totalTokensAll)
                                        .totalCalls(totalCallsAll)
                                        .rankingType("模型Token消耗排行榜")
                                        .mostActiveModel(mostActiveModel)
                                        .mostEfficientModel(mostEfficientModel)
                                        .build();

                } catch (Exception e) {
                        log.error("获取模型Token排行榜失败: error={}", e.getMessage(), e);
                        return null;
                }
        }

        @Override
        public List<ModelTokenSummaryDTO> getAllModelTokenSummaries(LocalDateTime startTime, LocalDateTime endTime) {
                try {
                        QueryWrapper queryWrapper = QueryWrapper.create();
                        if (startTime != null) {
                                queryWrapper.and("createTime >= ?", startTime);
                        }
                        if (endTime != null) {
                                queryWrapper.and("createTime <= ?", endTime);
                        }

                        List<TokenUsage> allTokens = list(queryWrapper);

                        // 按模型名称分组
                        Map<String, List<TokenUsage>> modelGroups = allTokens.stream()
                                        .filter(t -> t.getModelName() != null)
                                        .collect(Collectors.groupingBy(TokenUsage::getModelName));

                        List<ModelTokenSummaryDTO> result = new ArrayList<>();

                        for (Map.Entry<String, List<TokenUsage>> entry : modelGroups.entrySet()) {
                                String modelName = entry.getKey();
                                List<TokenUsage> modelTokens = entry.getValue();

                                // 计算统计数据
                                long totalInput = modelTokens.stream()
                                                .mapToLong(t -> t.getRequestTokenCount() != null
                                                                ? t.getRequestTokenCount()
                                                                : 0)
                                                .sum();
                                long totalOutput = modelTokens.stream()
                                                .mapToLong(t -> t.getResponseTokenCount() != null
                                                                ? t.getResponseTokenCount()
                                                                : 0)
                                                .sum();
                                long totalTokens = modelTokens.stream()
                                                .mapToLong(t -> t.getTotalTokenCount() != null ? t.getTotalTokenCount()
                                                                : 0)
                                                .sum();

                                long callCount = modelTokens.size();
                                long userCount = modelTokens.stream().map(TokenUsage::getUserId).distinct().count();
                                long appCount = modelTokens.stream().map(TokenUsage::getAppId).distinct().count();

                                double avgTokensPerCall = callCount > 0 ? (double) totalTokens / callCount : 0.0;
                                double avgTokensPerUser = userCount > 0 ? (double) totalTokens / userCount : 0.0;

                                // 获取时间范围
                                LocalDateTime firstUsed = modelTokens.stream()
                                                .map(TokenUsage::getCreateTime)
                                                .filter(Objects::nonNull)
                                                .min(LocalDateTime::compareTo)
                                                .orElse(null);
                                LocalDateTime lastUsed = modelTokens.stream()
                                                .map(TokenUsage::getCreateTime)
                                                .filter(Objects::nonNull)
                                                .max(LocalDateTime::compareTo)
                                                .orElse(null);

                                // 生成用途分布
                                String purposeDistribution = generatePurposeDistribution(modelTokens);

                                // 估算成本
                                double estimatedCost = calculateEstimatedCost(modelName, totalInput, totalOutput);

                                ModelTokenSummaryDTO summary = ModelTokenSummaryDTO.builder()
                                                .modelName(modelName)
                                                .modelDisplayName(getModelDisplayName(modelName))
                                                .totalInputTokens(totalInput)
                                                .totalOutputTokens(totalOutput)
                                                .totalTokens(totalTokens)
                                                .callCount(callCount)
                                                .userCount(userCount)
                                                .appCount(appCount)
                                                .avgTokensPerCall(avgTokensPerCall)
                                                .avgTokensPerUser(avgTokensPerUser)
                                                .firstUsedTime(firstUsed)
                                                .lastUsedTime(lastUsed)
                                                .purposeDistribution(purposeDistribution)
                                                .modelProvider(getModelProvider(modelName))
                                                .estimatedCost(estimatedCost)
                                                .build();

                                result.add(summary);
                        }

                        return result;

                } catch (Exception e) {
                        log.error("获取所有模型Token汇总失败: error={}", e.getMessage(), e);
                        return new ArrayList<>();
                }
        }

        /**
         * 获取模型显示名称
         */
        private String getModelDisplayName(String modelName) {
                if (modelName == null)
                        return "未知模型";
                switch (modelName.toLowerCase()) {
                        case "gpt-4o-mini": return "GPT-4o Mini";
                        case "gpt-4o": return "GPT-4o";
                        case "gpt-4-turbo": return "GPT-4 Turbo";
                        case "gpt-3.5-turbo": return "GPT-3.5 Turbo";
                        case "claude-3-haiku": return "Claude-3 Haiku";
                        case "claude-3-sonnet": return "Claude-3 Sonnet";
                        case "claude-3-opus": return "Claude-3 Opus";
                        default: return modelName;
                }
        }

        /**
         * 获取模型提供商
         */
        private String getModelProvider(String modelName) {
                if (modelName == null)
                        return "未知";
                String lowerName = modelName.toLowerCase();
                if (lowerName.contains("gpt"))
                        return "OpenAI";
                if (lowerName.contains("claude"))
                        return "Anthropic";
                if (lowerName.contains("gemini"))
                        return "Google";
                if (lowerName.contains("llama"))
                        return "Meta";
                return "其他";
        }

        /**
         * 生成用途分布字符串
         */
        private String generatePurposeDistribution(List<TokenUsage> tokens) {
                Map<String, Long> purposeMap = tokens.stream()
                                .collect(Collectors.groupingBy(
                                                t -> t.getAppType() != null ? t.getAppType() : "UNKNOWN",
                                                Collectors.summingLong(t -> t.getTotalTokenCount() != null
                                                                ? t.getTotalTokenCount()
                                                                : 0)));

                long total = purposeMap.values().stream().mapToLong(Long::longValue).sum();
                if (total == 0)
                        return "无数据";

                return purposeMap.entrySet().stream()
                                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                .map(entry -> getPurposeDescription(entry.getKey()) +
                                                ":" + String.format("%.1f%%", (double) entry.getValue() / total * 100))
                                .collect(Collectors.joining(","));
        }

        /**
         * 估算成本（USD）
         * 这里使用简化的估算，实际应用中需要根据具体的模型定价
         */
        private double calculateEstimatedCost(String modelName, long inputTokens, long outputTokens) {
                if (modelName == null)
                        return 0.0;

                // 简化的定价表（每1000个token的价格，USD）
                double inputPrice = 0.0;
                double outputPrice = 0.0;

                String lowerName = modelName.toLowerCase();
                if (lowerName.contains("gpt-4o-mini")) {
                        inputPrice = 0.00015; // $0.15 per 1M tokens
                        outputPrice = 0.0006; // $0.60 per 1M tokens
                } else if (lowerName.contains("gpt-4o")) {
                        inputPrice = 0.005; // $5.00 per 1M tokens
                        outputPrice = 0.015; // $15.00 per 1M tokens
                } else if (lowerName.contains("gpt-4")) {
                        inputPrice = 0.01; // $10.00 per 1M tokens
                        outputPrice = 0.03; // $30.00 per 1M tokens
                } else if (lowerName.contains("gpt-3.5")) {
                        inputPrice = 0.0005; // $0.50 per 1M tokens
                        outputPrice = 0.0015; // $1.50 per 1M tokens
                } else {
                        // 默认使用中等价格
                        inputPrice = 0.001;
                        outputPrice = 0.002;
                }

                return (inputTokens * inputPrice / 1000) + (outputTokens * outputPrice / 1000);
        }

        /**
         * 获取用途描述
         */
        private String getPurposeDescription(String purpose) {
                if (purpose == null)
                        return "未知";
                switch (purpose) {
                        case "ROUTING": return "AI路由决策";
                        case "CODE_GENERATION": return "代码生成";
                        case "INPUT_SAFETY_CHECK": return "输入安全检查";
                        case "CHAT_INTERACTION": return "聊天交互";
                        default: return purpose;
                }
        }

        @Override
        public GenerationStatsDTO getUserGenerationStats(Long userId) {
                try {
                        // 统计代码生成次数和 Token 消耗
                        List<TokenUsage> codeGenRecords = list(QueryWrapper.create()
                                        .where("userId = ?", userId)
                                        .and("aiCallPurpose = ?", "CODE_GENERATION"));

                        long totalGenerations = codeGenRecords.stream()
                                        .map(TokenUsage::getAppId)
                                        .distinct()
                                        .count();
                        long totalTokens = codeGenRecords.stream()
                                        .mapToLong(t -> t.getTotalTokenCount() != null ? t.getTotalTokenCount() : 0)
                                        .sum();

                        // 按应用类型统计
                        List<App> userApps = appService.list(QueryWrapper.create()
                                        .where("userId = ?", userId));

                        long htmlCount = userApps.stream()
                                        .filter(a -> "html".equals(a.getCodeGenType())).count();
                        long multiFileCount = userApps.stream()
                                        .filter(a -> "multi_file".equals(a.getCodeGenType())).count();
                        long vueCount = userApps.stream()
                                        .filter(a -> "vue_project".equals(a.getCodeGenType())).count();
                        long deployCount = userApps.stream()
                                        .filter(a -> a.getDeployedTime() != null).count();

                        // 最近生成记录（取最近 5 条）
                        List<GenerationStatsDTO.RecentGeneration> recentRecords = codeGenRecords.stream()
                                        .sorted(Comparator.comparing(TokenUsage::getCreateTime,
                                                        Comparator.nullsLast(Comparator.reverseOrder())))
                                        .limit(5)
                                        .map(t -> {
                                                App app = userApps.stream()
                                                                .filter(a -> a.getId().equals(t.getAppId()))
                                                                .findFirst().orElse(null);
                                                return GenerationStatsDTO.RecentGeneration.builder()
                                                                .appId(t.getAppId())
                                                                .appName(app != null ? app.getAppName() : "未知应用")
                                                                .codeGenType(app != null ? app.getCodeGenType() : "")
                                                                .tokenUsed(t.getTotalTokenCount() != null
                                                                                ? t.getTotalTokenCount().longValue()
                                                                                : 0L)
                                                                .createTime(t.getCreateTime() != null
                                                                                ? t.getCreateTime().toString()
                                                                                : "")
                                                                .build();
                                        })
                                        .collect(Collectors.toList());

                        return GenerationStatsDTO.builder()
                                        .totalGenerations(totalGenerations)
                                        .totalTokens(totalTokens)
                                        .htmlCount(htmlCount)
                                        .multiFileCount(multiFileCount)
                                        .vueCount(vueCount)
                                        .deployCount(deployCount)
                                        .recentRecords(recentRecords)
                                        .build();

                } catch (Exception e) {
                        log.error("获取用户生成统计失败: userId={}, error={}", userId, e.getMessage(), e);
                        return GenerationStatsDTO.builder()
                                        .totalGenerations(0L).totalTokens(0L)
                                        .htmlCount(0L).multiFileCount(0L).vueCount(0L).deployCount(0L)
                                        .recentRecords(Collections.emptyList())
                                        .build();
                }
        }
}