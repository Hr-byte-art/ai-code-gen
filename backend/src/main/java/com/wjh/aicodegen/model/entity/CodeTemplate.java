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
 * 代码模板 实体类。
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("code_template")
public class CodeTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 模板名称
     */
    @Column("template_name")
    private String templateName;

    /**
     * 模板标识
     */
    @Column("template_key")
    private String templateKey;

    /**
     * 模板描述
     */
    @Column("description")
    private String description;

    /**
     * 生成类型: html/multi_file/vue_project
     */
    @Column("code_gen_type")
    private String codeGenType;

    /**
     * 模板代码内容(JSON格式)
     */
    @Column("template_content")
    private String templateContent;

    /**
     * 预览图URL
     */
    @Column("preview_url")
    private String previewUrl;

    /**
     * 使用次数
     */
    @Column("use_count")
    private Integer useCount;

    /**
     * 状态: 0-禁用 1-启用
     */
    @Column("status")
    private Integer status;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}
