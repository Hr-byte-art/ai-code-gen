package com.wjh.aicodegen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.AgentTraceMapper;
import com.wjh.aicodegen.model.entity.AgentTrace;
import com.wjh.aicodegen.model.vo.AgentTraceSummaryVO;
import com.wjh.aicodegen.service.AgentTraceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 执行追踪 服务实现
 */
@Service
public class AgentTraceServiceImpl extends ServiceImpl<AgentTraceMapper, AgentTrace> implements AgentTraceService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    @Override
    public List<AgentTrace> getByAppId(Long appId) {
        return list(QueryWrapper.create()
                .where("app_id = ?", appId)
                .orderBy("create_time", false));
    }

    @Override
    public List<AgentTrace> getByTraceId(String traceId) {
        return list(QueryWrapper.create()
                .where("trace_id = ?", traceId)
                .orderBy("create_time", true));
    }

    @Override
    public List<AgentTrace> getRecent(int limit) {
        return list(QueryWrapper.create()
                .orderBy("create_time", false)
                .limit(normalizeLimit(limit)));
    }

    @Override
    public List<AgentTrace> listByCondition(Long appId, String traceId, String agentName, String status,
                                            String reviewResult, int limit) {
        QueryWrapper queryWrapper = buildConditionQuery(appId, traceId, agentName, status, reviewResult)
                .orderBy("create_time", false)
                .limit(normalizeLimit(limit));
        return list(queryWrapper);
    }

    @Override
    public List<AgentTraceSummaryVO> listSummaries(Long appId, String traceId, String status,
                                                   String reviewResult, int limit) {
        List<AgentTrace> traces = listByCondition(appId, traceId, null, status, reviewResult, limit);
        Map<String, List<AgentTrace>> tracesByTraceId = traces.stream()
                .filter(trace -> StrUtil.isNotBlank(trace.getTraceId()))
                .collect(LinkedHashMap::new,
                        (map, trace) -> map.computeIfAbsent(trace.getTraceId(), key -> new java.util.ArrayList<>()).add(trace),
                        Map::putAll);

        return tracesByTraceId.values().stream()
                .map(this::buildSummary)
                .sorted(Comparator.comparing(AgentTraceSummaryVO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private QueryWrapper buildConditionQuery(Long appId, String traceId, String agentName, String status,
                                             String reviewResult) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (appId != null) {
            queryWrapper.where("app_id = ?", appId);
        }
        if (StrUtil.isNotBlank(traceId)) {
            queryWrapper.and("trace_id = ?", traceId.trim());
        }
        if (StrUtil.isNotBlank(agentName)) {
            queryWrapper.and("agent_name = ?", agentName.trim());
        }
        if (StrUtil.isNotBlank(status)) {
            queryWrapper.and("status = ?", status.trim());
        }
        if (StrUtil.isNotBlank(reviewResult)) {
            queryWrapper.and("review_result = ?", reviewResult.trim());
        }
        return queryWrapper;
    }

    private AgentTraceSummaryVO buildSummary(List<AgentTrace> traces) {
        List<AgentTrace> orderedTraces = traces.stream()
                .sorted(Comparator.comparing(AgentTrace::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        AgentTrace firstTrace = orderedTraces.get(0);
        AgentTrace lastTrace = orderedTraces.get(orderedTraces.size() - 1);
        AgentTrace finalReviewTrace = findFinalReviewTrace(orderedTraces);
        boolean hasError = orderedTraces.stream().anyMatch(trace -> !"success".equals(trace.getStatus()));
        long totalDurationMs = orderedTraces.stream()
                .map(AgentTrace::getDurationMs)
                .filter(duration -> duration != null && duration > 0)
                .mapToLong(Long::longValue)
                .sum();

        return AgentTraceSummaryVO.builder()
                .traceId(firstTrace.getTraceId())
                .appId(firstTrace.getAppId())
                .userId(firstTrace.getUserId())
                .status(hasError ? "error" : "success")
                .finalReviewResult(finalReviewTrace != null ? finalReviewTrace.getReviewResult() : null)
                .finalReviewScore(finalReviewTrace != null ? finalReviewTrace.getReviewScore() : null)
                .stepCount(orderedTraces.size())
                .totalDurationMs(totalDurationMs)
                .startTime(resolveStartTime(firstTrace))
                .endTime(resolveEndTime(lastTrace))
                .createTime(lastTrace.getCreateTime())
                .hasError(hasError)
                .build();
    }

    private AgentTrace findFinalReviewTrace(List<AgentTrace> orderedTraces) {
        for (int i = orderedTraces.size() - 1; i >= 0; i--) {
            AgentTrace trace = orderedTraces.get(i);
            if (StrUtil.isNotBlank(trace.getReviewResult()) || trace.getReviewScore() != null) {
                return trace;
            }
        }
        return null;
    }

    private LocalDateTime resolveStartTime(AgentTrace trace) {
        return trace.getStartTime() != null ? trace.getStartTime() : trace.getCreateTime();
    }

    private LocalDateTime resolveEndTime(AgentTrace trace) {
        return trace.getEndTime() != null ? trace.getEndTime() : trace.getCreateTime();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
