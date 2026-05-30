package com.wjh.aicodegen.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 工具定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {

    /** 工具名称 */
    private String name;

    /** 工具描述 */
    private String description;

    /** 参数 JSON Schema */
    private Map<String, Object> inputSchema;

    /** 所属 MCP 服务器名称 */
    private String serverName;

    /** 所属 MCP 服务器 URL */
    private String serverUrl;
}
