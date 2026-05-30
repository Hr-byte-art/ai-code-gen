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
 * 代码生成技能 实体类。
 * 将 prompt + 工具集 + 构建策略抽象为可配置的单元。
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("code_skill")
public class CodeSkill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 技能名称 */
    @Column("name")
    private String name;

    /** 唯一标识 */
    @Column("skill_key")
    private String skillKey;

    /** 技能描述 */
    @Column("description")
    private String description;

    /** 系统提示词 */
    @Column("system_prompt")
    private String systemPrompt;

    /** 代码生成类型 */
    @Column("code_gen_type")
    private String codeGenType;

    /** 积分消耗 */
    @Column("point_cost")
    private Integer pointCost;

    /** 可用工具列表，逗号分隔，null 表示全部 */
    @Column("tool_names")
    private String toolNames;

    /** 构建策略: none/vue/fullstack */
    @Column("build_strategy")
    private String buildStrategy;

    /** 模型策略: standard/reasoning */
    @Column("model_strategy")
    private String modelStrategy;

    /** 是否启用 */
    @Column("is_active")
    private Integer isActive;

    /** 排序 */
    @Column("sort_order")
    private Integer sortOrder;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;

    /** 内容哈希（用于变更检测） */
    @Column("content_hash")
    private String contentHash;

    /** 数据来源: file=文件, manual=手动创建 */
    @Column("source")
    private String source;

    /** 自定义工具定义（JSON 数组） */
    @Column("custom_tools")
    private String customTools;

    /** 生命周期钩子定义（JSON 对象） */
    @Column("hooks")
    private String hooks;

    /** 是否公开到市场 */
    @Column("is_public")
    private Integer isPublic;

    /** 使用次数 */
    @Column("use_count")
    private Integer useCount;

    /** 作者用户 ID */
    @Column("author_id")
    private Long authorId;

    /** 平均评分（1-5） */
    @Column("rating_avg")
    private Double ratingAvg;

    /** 评分人数 */
    @Column("rating_count")
    private Integer ratingCount;
}
