package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.AgentTraceMapper;
import com.wjh.aicodegen.model.entity.AgentTrace;
import com.wjh.aicodegen.service.AgentTraceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 执行追踪 服务实现
 */
@Service
public class AgentTraceServiceImpl extends ServiceImpl<AgentTraceMapper, AgentTrace> implements AgentTraceService {

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
                .limit(limit));
    }
}
