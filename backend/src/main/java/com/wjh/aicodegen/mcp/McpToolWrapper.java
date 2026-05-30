package com.wjh.aicodegen.mcp;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP 工具包装器
 * 将 MCP 工具暴露为 LangChain4j 可调用的 @Tool 方法
 * AI 通过 callMcpTool(toolName, params) 调用任意 MCP 工具
 */
@Slf4j
@Component
public class McpToolWrapper {

    private final McpToolRegistry mcpToolRegistry;

    public McpToolWrapper(McpToolRegistry mcpToolRegistry) {
        this.mcpToolRegistry = mcpToolRegistry;
    }

    /**
     * AI 调用 MCP 工具的统一入口
     *
     * @param toolFullName 格式为 "serverName:toolName"
     * @param params       JSON 格式的参数
     * @return 工具执行结果
     */
    @Tool("调用外部 MCP 工具。toolName 格式为 'serverName:toolName'，params 为 JSON 格式参数。" +
          "可用工具通过 listMcpTools 获取。")
    public String callMcpTool(
            @P("工具名称，格式: serverName:toolName") String toolName,
            @P("JSON 格式的参数") String params) {

        if (toolName == null || toolName.isBlank()) {
            return "请提供工具名称";
        }

        log.info("调用 MCP 工具: tool={}, params={}", toolName, params);
        String result = mcpToolRegistry.callTool(toolName, params != null ? params : "{}");
        log.info("MCP 工具结果: tool={}, result={}", toolName,
                result.length() > 200 ? result.substring(0, 200) + "..." : result);
        return result;
    }

    /**
     * 列出所有可用的 MCP 工具
     */
    @Tool("列出所有可用的 MCP 外部工具及其参数说明")
    public String listMcpTools() {
        var tools = mcpToolRegistry.getAllTools();
        if (tools.isEmpty()) {
            return "当前没有可用的 MCP 工具";
        }

        StringBuilder sb = new StringBuilder("可用的 MCP 工具：\n\n");
        for (McpTool tool : tools) {
            sb.append("- **").append(tool.getServerName()).append(":").append(tool.getName()).append("**\n");
            sb.append("  ").append(tool.getDescription()).append("\n");
            if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                sb.append("  参数: ").append(tool.getInputSchema()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
