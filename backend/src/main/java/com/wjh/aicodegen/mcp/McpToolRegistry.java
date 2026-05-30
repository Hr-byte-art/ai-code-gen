package com.wjh.aicodegen.mcp;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册表
 * 管理所有 MCP 服务器的工具，提供统一的调用入口
 */
@Slf4j
@Component
public class McpToolRegistry {

    /** 服务器名称 -> McpClient */
    private final ConcurrentHashMap<String, McpClient> clients = new ConcurrentHashMap<>();

    /** 工具全名(serverName:toolName) -> McpTool */
    private final ConcurrentHashMap<String, McpTool> tools = new ConcurrentHashMap<>();

    /**
     * 注册 MCP 服务器
     */
    public void registerServer(String serverUrl, String serverName) {
        McpClient client = new McpClient(serverUrl, serverName);

        // 初始化连接
        client.initialize();

        // 发现工具
        List<McpTool> discoveredTools = client.listTools();
        for (McpTool tool : discoveredTools) {
            String fullName = serverName + ":" + tool.getName();
            tools.put(fullName, tool);
        }

        clients.put(serverName, client);
        log.info("MCP 服务器已注册: name={}, url={}, tools={}", serverName, serverUrl, discoveredTools.size());
    }

    /**
     * 注销 MCP 服务器
     */
    public void unregisterServer(String serverName) {
        clients.remove(serverName);
        tools.entrySet().removeIf(entry -> entry.getKey().startsWith(serverName + ":"));
        log.info("MCP 服务器已注销: name={}", serverName);
    }

    /**
     * 调用 MCP 工具
     */
    public String callTool(String toolFullName, String argsJson) {
        McpTool tool = tools.get(toolFullName);
        if (tool == null) {
            return "未知的 MCP 工具: " + toolFullName;
        }

        McpClient client = clients.get(tool.getServerName());
        if (client == null) {
            return "MCP 服务器未连接: " + tool.getServerName();
        }

        try {
            Map<String, Object> args = cn.hutool.json.JSONUtil.toBean(argsJson, Map.class);
            return client.callTool(tool.getName(), args);
        } catch (Exception e) {
            log.error("MCP 工具调用失败: tool={}, error={}", toolFullName, e.getMessage());
            return "工具调用失败: " + e.getMessage();
        }
    }

    /**
     * 获取所有已注册的 MCP 工具
     */
    public Collection<McpTool> getAllTools() {
        return tools.values();
    }

    /**
     * 获取指定服务器的工具
     */
    public List<McpTool> getToolsByServer(String serverName) {
        return tools.entrySet().stream()
                .filter(e -> e.getKey().startsWith(serverName + ":"))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * 获取已注册的服务器列表
     */
    public Set<String> getRegisteredServers() {
        return clients.keySet();
    }

    /**
     * 刷新指定服务器的工具列表
     */
    public void refreshServer(String serverName) {
        McpClient client = clients.get(serverName);
        if (client == null) return;

        // 清除旧工具
        tools.entrySet().removeIf(entry -> entry.getKey().startsWith(serverName + ":"));

        // 重新发现
        List<McpTool> discoveredTools = client.listTools();
        for (McpTool tool : discoveredTools) {
            String fullName = serverName + ":" + tool.getName();
            tools.put(fullName, tool);
        }

        log.info("MCP 服务器工具已刷新: name={}, tools={}", serverName, discoveredTools.size());
    }
}
