package com.wjh.aicodegen.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 执行追踪实体
 * 记录每次 Agent 调用的耗时、Token 消耗、状态等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_trace")
public class AgentTrace implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 追踪 ID（同一次生成流程共享） */
    @Column("trace_id")
    private String traceId;

    /** Agent 名称：review / optimizer / generator / router */
    @Column("agent_name")
    private String agentName;

    /** 关联应用 ID */
    @Column("app_id")
    private Long appId;

    /** 关联用户 ID */
    @Column("user_id")
    private Long userId;

    /** 使用的模型名称 */
    @Column("model_name")
    private String modelName;

    /** 输入 Token 数 */
    @Column("input_tokens")
    private Integer inputTokens;

    /** 输出 Token 数 */
    @Column("output_tokens")
    private Integer outputTokens;

    /** 执行状态：success / error / timeout */
    @Column("status")
    private String status;

    /** 审查结果：passed / failed / skipped */
    @Column("review_result")
    private String reviewResult;

    /** 审查评分（0-100） */
    @Column("review_score")
    private Integer reviewScore;

    /** 问题列表（JSON） */
    @Column("issues")
    private String issues;

    /** 开始时间 */
    @Column("start_time")
    private LocalDateTime startTime;

    /** 结束时间 */
    @Column("end_time")
    private LocalDateTime endTime;

    /** 执行耗时（毫秒） */
    @Column("duration_ms")
    private Long durationMs;

    /** 错误信息 */
    @Column("error_message")
    private String errorMessage;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}
