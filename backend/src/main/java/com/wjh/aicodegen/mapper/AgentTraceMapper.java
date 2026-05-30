package com.wjh.aicodegen.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wjh.aicodegen.model.entity.AgentTrace;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 执行追踪 Mapper
 */
@Mapper
public interface AgentTraceMapper extends BaseMapper<AgentTrace> {
}
