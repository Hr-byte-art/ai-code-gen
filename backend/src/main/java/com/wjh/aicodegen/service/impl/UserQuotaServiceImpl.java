package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.UserQuotaMapper;
import com.wjh.aicodegen.mapper.TokenUsageMapper;
import com.wjh.aicodegen.model.entity.UserQuota;
import com.wjh.aicodegen.model.vo.UserQuotaVO;
import com.wjh.aicodegen.service.UserQuotaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户配额配置 服务实现。
 *
 * @author 王哈哈
 */
@Service
@Slf4j
public class UserQuotaServiceImpl extends ServiceImpl<UserQuotaMapper, UserQuota> implements UserQuotaService {

    @Resource
    private TokenUsageMapper tokenUsageMapper;

    @Override
    public UserQuota getQuotaByRole(String role) {
        return mapper.selectOneByQuery(
                QueryWrapper.create()
                        .from("user_quota")
                        .where("user_role = ?", role)
        );
    }

    @Override
    public boolean checkQuota(Long userId, String role) {
        UserQuota quota = getQuotaByRole(role);
        if (quota == null) {
            return true;
        }
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        long dailyGenCount = countGenerationRecords(userId, todayStart);
        if (dailyGenCount >= quota.getDailyGenLimit()) {
            log.warn("用户 {} 今日生成次数已达上限: {}/{}", userId, dailyGenCount, quota.getDailyGenLimit());
            return false;
        }

        long monthlyGenCount = countGenerationRecords(userId, monthStart);
        if (monthlyGenCount >= quota.getMonthlyGenLimit()) {
            log.warn("用户 {} 本月生成次数已达上限: {}/{}", userId, monthlyGenCount, quota.getMonthlyGenLimit());
            return false;
        }

        long dailyTokenUsage = sumTokenUsage(userId, todayStart);
        if (dailyTokenUsage >= quota.getDailyTokenLimit()) {
            log.warn("用户 {} 今日 Token 消耗已达上限: {}/{}", userId, dailyTokenUsage, quota.getDailyTokenLimit());
            return false;
        }

        long monthlyTokenUsage = sumTokenUsage(userId, monthStart);
        if (monthlyTokenUsage >= quota.getMonthlyTokenLimit()) {
            log.warn("用户 {} 本月 Token 消耗已达上限: {}/{}", userId, monthlyTokenUsage, quota.getMonthlyTokenLimit());
            return false;
        }

        return true;
    }

    @Override
    public UserQuotaVO getUserQuotaVO(Long userId, String role) {
        UserQuota quota = getQuotaByRole(role);
        if (quota == null) {
            return UserQuotaVO.builder()
                    .dailyGenUsed(0).dailyGenLimit(9999)
                    .monthlyGenUsed(0).monthlyGenLimit(99999)
                    .dailyTokenUsed(0L).dailyTokenLimit(999999999L)
                    .monthlyTokenUsed(0L).monthlyTokenLimit(999999999L)
                    .build();
        }

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        return UserQuotaVO.builder()
                .dailyGenUsed((int) countGenerationRecords(userId, todayStart))
                .dailyGenLimit(quota.getDailyGenLimit())
                .monthlyGenUsed((int) countGenerationRecords(userId, monthStart))
                .monthlyGenLimit(quota.getMonthlyGenLimit())
                .dailyTokenUsed(sumTokenUsage(userId, todayStart))
                .dailyTokenLimit(quota.getDailyTokenLimit().longValue())
                .monthlyTokenUsed(sumTokenUsage(userId, monthStart))
                .monthlyTokenLimit(quota.getMonthlyTokenLimit().longValue())
                .build();
    }

    private long countGenerationRecords(Long userId, LocalDateTime since) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("COUNT(DISTINCT appId)")
                .from("token_usage_record")
                .where("userId = ?", userId)
                .and("appId > ?", 0)
                .and("aiCallPurpose = ?", "CODE_GENERATION")
                .and("createTime >= ?", since);
        Long count = tokenUsageMapper.selectObjectByQueryAs(wrapper, Long.class);
        return count != null ? count : 0L;
    }

    private long sumTokenUsage(Long userId, LocalDateTime since) {
        QueryWrapper wrapper = QueryWrapper.create()
                .select("COALESCE(SUM(totalTokens), 0)")
                .from("token_usage_record")
                .where("userId = ?", userId)
                .and("appId > ?", 0)
                .and("createTime >= ?", since);
        Long sum = tokenUsageMapper.selectObjectByQueryAs(wrapper, Long.class);
        return sum != null ? sum : 0L;
    }
}
