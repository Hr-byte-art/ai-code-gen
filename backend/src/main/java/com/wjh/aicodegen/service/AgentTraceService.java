package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.entity.AgentTrace;

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
}
