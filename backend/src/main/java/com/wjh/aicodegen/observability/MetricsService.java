package com.wjh.aicodegen.observability;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 自定义 Prometheus 指标服务
 *
 * 指标列表：
 * - agent_execution_total: Agent 执行次数 (tags: agent_name, status)
 * - agent_execution_duration: Agent 执行耗时 (tags: agent_name)
 * - agent_token_usage: Agent Token 消耗 (tags: agent_name, token_type)
 * - skill_usage_total: Skill 使用次数 (tags: skill_key)
 * - skill_generation_duration: Skill 生成耗时 (tags: skill_key)
 * - tool_call_total: 工具调用次数 (tags: tool_name, status)
 * - tool_call_duration: 工具调用耗时 (tags: tool_name)
 * - app_deploy_total: 应用部署次数 (tags: code_gen_type, status)
 * - code_generation_total: 代码生成次数 (tags: code_gen_type, status)
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Agent 指标
    private final Counter agentExecutionTotal;
    private final Timer agentExecutionDuration;
    private final Counter agentTokenUsage;

    // Skill 指标
    private final Counter skillUsageTotal;
    private final Timer skillGenerationDuration;

    // 工具调用指标
    private final Counter toolCallTotal;
    private final Timer toolCallDuration;

    // 应用指标
    private final Counter appDeployTotal;
    private final Counter codeGenerationTotal;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Agent 执行指标
        this.agentExecutionTotal = Counter.builder("agent_execution_total")
                .description("Agent 执行总次数")
                .tag("agent_name", "all")
                .tag("status", "all")
                .register(meterRegistry);

        this.agentExecutionDuration = Timer.builder("agent_execution_duration_seconds")
                .description("Agent 执行耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.agentTokenUsage = Counter.builder("agent_token_usage_total")
                .description("Agent Token 消耗总量")
                .tag("agent_name", "all")
                .tag("token_type", "all")
                .register(meterRegistry);

        // Skill 使用指标
        this.skillUsageTotal = Counter.builder("skill_usage_total")
                .description("Skill 使用总次数")
                .tag("skill_key", "all")
                .register(meterRegistry);

        this.skillGenerationDuration = Timer.builder("skill_generation_duration_seconds")
                .description("Skill 代码生成耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // 工具调用指标
        this.toolCallTotal = Counter.builder("tool_call_total")
                .description("工具调用总次数")
                .tag("tool_name", "all")
                .tag("status", "all")
                .register(meterRegistry);

        this.toolCallDuration = Timer.builder("tool_call_duration_seconds")
                .description("工具调用耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // 应用指标
        this.appDeployTotal = Counter.builder("app_deploy_total")
                .description("应用部署总次数")
                .tag("code_gen_type", "all")
                .tag("status", "all")
                .register(meterRegistry);

        this.codeGenerationTotal = Counter.builder("code_generation_total")
                .description("代码生成总次数")
                .tag("code_gen_type", "all")
                .tag("status", "all")
                .register(meterRegistry);
    }

    /**
     * 记录 Agent 执行
     */
    public void recordAgentExecution(String agentName, String status, long durationMs) {
        Counter.builder("agent_execution_total")
                .description("Agent 执行总次数")
                .tag("agent_name", agentName)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        Timer.builder("agent_execution_duration_seconds")
                .description("Agent 执行耗时")
                .tag("agent_name", agentName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 Agent Token 消耗
     */
    public void recordAgentTokenUsage(String agentName, String tokenType, int tokens) {
        Counter.builder("agent_token_usage_total")
                .description("Agent Token 消耗总量")
                .tag("agent_name", agentName)
                .tag("token_type", tokenType)
                .register(meterRegistry)
                .increment(tokens);
    }

    /**
     * 记录 Skill 使用
     */
    public void recordSkillUsage(String skillKey) {
        Counter.builder("skill_usage_total")
                .description("Skill 使用总次数")
                .tag("skill_key", skillKey)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 Skill 生成耗时
     */
    public void recordSkillGenerationDuration(String skillKey, long durationMs) {
        Timer.builder("skill_generation_duration_seconds")
                .description("Skill 代码生成耗时")
                .tag("skill_key", skillKey)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录工具调用
     */
    public void recordToolCall(String toolName, String status, long durationMs) {
        Counter.builder("tool_call_total")
                .description("工具调用总次数")
                .tag("tool_name", toolName)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        Timer.builder("tool_call_duration_seconds")
                .description("工具调用耗时")
                .tag("tool_name", toolName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录应用部署
     */
    public void recordAppDeploy(String codeGenType, String status) {
        Counter.builder("app_deploy_total")
                .description("应用部署总次数")
                .tag("code_gen_type", codeGenType)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录代码生成
     */
    public void recordCodeGeneration(String codeGenType, String status) {
        Counter.builder("code_generation_total")
                .description("代码生成总次数")
                .tag("code_gen_type", codeGenType)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }
}
