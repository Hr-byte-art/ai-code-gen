package com.wjh.aicodegen.mcp;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP (Model Context Protocol) 客户端
 * 通过 HTTP/SSE 传输连接 MCP 服务器
 *
 * 支持的 JSON-RPC 方法：
 * - initialize: 初始化连接
 * - tools/list: 列出可用工具
 * - tools/call: 调用工具
 */
@Slf4j
public class McpClient {

    private final String serverUrl;
    private final String serverName;
    private final AtomicLong requestId = new AtomicLong(1);
    private final int timeoutMs;

    public McpClient(String serverUrl, String serverName) {
        this(serverUrl, serverName, 10000);
    }

    public McpClient(String serverUrl, String serverName, int timeoutMs) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        this.serverName = serverName;
        this.timeoutMs = timeoutMs;
    }

    /**
     * 初始化 MCP 连接
     */
    public JSONObject initialize() {
        JSONObject params = new JSONObject();
        JSONObject clientInfo = new JSONObject();
        clientInfo.set("name", "ai-code-gen");
        clientInfo.set("version", "1.0.0");
        params.set("clientInfo", clientInfo);
        params.set("protocolVersion", "2024-11-05");

        return sendRequest("initialize", params);
    }

    /**
     * 列出服务器上的所有工具
     */
    public List<McpTool> listTools() {
        List<McpTool> tools = new ArrayList<>();

        JSONObject result = sendRequest("tools/list", new JSONObject());
        if (result == null) {
            log.warn("MCP tools/list 返回空: server={}", serverName);
            return tools;
        }

        JSONArray toolsArray = result.getJSONArray("tools");
        if (toolsArray == null) return tools;

        for (int i = 0; i < toolsArray.size(); i++) {
            JSONObject toolJson = toolsArray.getJSONObject(i);
            McpTool tool = McpTool.builder()
                    .name(toolJson.getStr("name"))
                    .description(toolJson.getStr("description", ""))
                    .inputSchema(toolJson.get("inputSchema") instanceof Map
                            ? (Map<String, Object>) toolJson.get("inputSchema")
                            : parseInputSchema(toolJson.getJSONObject("inputSchema")))
                    .serverName(serverName)
                    .serverUrl(serverUrl)
                    .build();
            tools.add(tool);
        }

        log.info("MCP 服务器发现 {} 个工具: server={}", tools.size(), serverName);
        return tools;
    }

    /**
     * 调用工具
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        JSONObject params = new JSONObject();
        params.set("name", toolName);
        params.set("arguments", arguments);

        JSONObject result = sendRequest("tools/call", params);
        if (result == null) {
            return "MCP 工具调用失败: " + toolName;
        }

        // 提取内容
        JSONArray content = result.getJSONArray("content");
        if (content != null && !content.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < content.size(); i++) {
                JSONObject item = content.getJSONObject(i);
                String type = item.getStr("type", "text");
                if ("text".equals(type)) {
                    sb.append(item.getStr("text", ""));
                } else {
                    sb.append("[").append(type).append("]");
                }
            }
            return sb.toString();
        }

        return result.toString();
    }

    /**
     * 发送 JSON-RPC 请求
     */
    private JSONObject sendRequest(String method, JSONObject params) {
        JSONObject request = new JSONObject();
        request.set("jsonrpc", "2.0");
        request.set("id", requestId.getAndIncrement());
        request.set("method", method);
        if (params != null && !params.isEmpty()) {
            request.set("params", params);
        }

        try {
            String url = serverUrl + "messages";
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(timeoutMs)
                    .body(request.toString())
                    .execute();

            if (response.isOk()) {
                JSONObject responseJson = JSONUtil.parseObj(response.body());

                // 检查错误
                if (responseJson.containsKey("error")) {
                    JSONObject error = responseJson.getJSONObject("error");
                    log.error("MCP 错误: server={}, code={}, message={}",
                            serverName, error.getInt("code"), error.getStr("message"));
                    return null;
                }

                return responseJson.getJSONObject("result");
            } else {
                log.error("MCP HTTP 错误: server={}, status={}", serverName, response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("MCP 请求异常: server={}, method={}, error={}", serverName, method, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseInputSchema(JSONObject schema) {
        if (schema == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public String getServerUrl() { return serverUrl; }
    public String getServerName() { return serverName; }
}
