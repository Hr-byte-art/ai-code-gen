package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.entity.AgentTrace;
import com.wjh.aicodegen.model.vo.AgentTraceSummaryVO;

import java.util.List;

/**
 * Agent 执行追踪 服务层
 */
public interface AgentTraceService extends IService<AgentTrace> {

    /**
     * 获取指定应用的追踪记录
     */
    List<AgentTrace> getByAppId(Long appId);

    /**
     * 获取指定追踪 ID 的所有记录
     */
    List<AgentTrace> getByTraceId(String traceId);

    /**
     * 获取最近的追踪记录
     */
    List<AgentTrace> getRecent(int limit);

    /**
     * 按条件获取追踪记录
     */
    List<AgentTrace> listByCondition(Long appId, String traceId, String agentName, String status,
                                     String reviewResult, int limit);

    /**
     * 按 traceId 聚合追踪链路
     */
    List<AgentTraceSummaryVO> listSummaries(Long appId, String traceId, String status,
                                            String reviewResult, int limit);
}
