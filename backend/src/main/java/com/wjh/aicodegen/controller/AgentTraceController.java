package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.model.entity.AgentTrace;
import com.wjh.aicodegen.model.vo.AgentTraceSummaryVO;
import com.wjh.aicodegen.service.AgentTraceService;
import com.wjh.aicodegen.utils.ResultUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 执行追踪接口
 */
@RestController
@RequestMapping("/agent/trace")
public class AgentTraceController {

    @Resource
    private AgentTraceService agentTraceService;

    /**
     * 获取最近的追踪记录（管理员）
     */
    @GetMapping("/admin/list")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<AgentTrace>> listRecent(@RequestParam(defaultValue = "100") int limit) {
        List<AgentTrace> traces = agentTraceService.getRecent(limit);
        return ResultUtils.success(traces);
    }

    /**
     * 按条件获取追踪记录（管理员）
     */
    @GetMapping("/admin/search")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<AgentTrace>> search(@RequestParam(required = false) Long appId,
                                                 @RequestParam(required = false) String traceId,
                                                 @RequestParam(required = false) String agentName,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String reviewResult,
                                                 @RequestParam(defaultValue = "200") int limit) {
        List<AgentTrace> traces = agentTraceService.listByCondition(
                appId, traceId, agentName, status, reviewResult, limit);
        return ResultUtils.success(traces);
    }

    /**
     * 按 traceId 聚合追踪链路（管理员）
     */
    @GetMapping("/admin/summary")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<AgentTraceSummaryVO>> listSummaries(@RequestParam(required = false) Long appId,
                                                                 @RequestParam(required = false) String traceId,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) String reviewResult,
                                                                 @RequestParam(defaultValue = "200") int limit) {
        List<AgentTraceSummaryVO> summaries = agentTraceService.listSummaries(
                appId, traceId, status, reviewResult, limit);
        return ResultUtils.success(summaries);
    }

    /**
     * 获取指定应用的追踪记录（管理员）
     */
    @GetMapping("/admin/app/{appId}")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<AgentTrace>> listByAppId(@PathVariable Long appId) {
        List<AgentTrace> traces = agentTraceService.getByAppId(appId);
        return ResultUtils.success(traces);
    }

    /**
     * 获取指定追踪 ID 的所有记录（管理员）
     */
    @GetMapping("/admin/trace/{traceId}")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<AgentTrace>> listByTraceId(@PathVariable String traceId) {
        List<AgentTrace> traces = agentTraceService.getByTraceId(traceId);
        return ResultUtils.success(traces);
    }
}
