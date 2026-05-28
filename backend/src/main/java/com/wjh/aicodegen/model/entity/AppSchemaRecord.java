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
 * 应用 Schema 记录 实体类。
 * 记录全栈应用创建的数据库表，用于删除应用时清理。
 *
 * @author 王哈哈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_schema_record")
public class AppSchemaRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用ID
     */
    @Column("app_id")
    private Long appId;

    /**
     * 表名（含前缀）
     */
    @Column("table_name")
    private String tableName;

    /**
     * 创建时间
     */
    @Column("create_time")
    private LocalDateTime createTime;
}
