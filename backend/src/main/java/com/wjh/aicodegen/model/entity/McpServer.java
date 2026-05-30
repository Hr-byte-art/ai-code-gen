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
 * MCP 服务器配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("mcp_server")
public class McpServer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 服务器名称（唯一） */
    @Column("name")
    private String name;

    /** 服务器 URL */
    @Column("url")
    private String url;

    /** 描述 */
    @Column("description")
    private String description;

    /** 传输类型: sse/stdio */
    @Column("transport")
    private String transport;

    /** 是否启用 */
    @Column("is_active")
    private Integer isActive;

    /** 工具数量（缓存） */
    @Column("tool_count")
    private Integer toolCount;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}
