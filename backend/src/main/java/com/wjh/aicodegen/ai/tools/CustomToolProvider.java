package com.wjh.aicodegen.ai.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 自定义工具分发器
 * 解析 Skill 定义的 HTTP 工具，提供统一的 @Tool 方法供 AI 调用
 *
 * 自定义工具定义格式（JSON 数组）：
 * [
 *   {
 *     "name": "qrGenerator",
 *     "description": "生成二维码图片 URL",
 *     "endpoint": "https://api.qrserver.com/v1/create-qr-code/?size={size}x{size}&data={text}",
 *     "method": "GET",
 *     "parameters": [
 *       {"name": "text", "type": "string", "description": "要编码的文本", "required": true},
 *       {"name": "size", "type": "integer", "description": "尺寸", "required": false, "default": "200"}
 *     ]
 *   }
 * ]
 */
@Slf4j
@Component
public class CustomToolProvider extends BaseTool {

    /** 工具名 -> 工具定义 */
    private final Map<String, CustomToolDefinition> toolDefMap = new ConcurrentHashMap<>();

    /**
     * 注册自定义工具（在 AI 服务创建时调用）
     */
    public void registerTools(String customToolsJson) {
        if (customToolsJson == null || customToolsJson.isBlank()) return;
        List<CustomToolDefinition> defs = parseToolDefinitions(customToolsJson);
        for (CustomToolDefinition def : defs) {
            toolDefMap.put(def.name, def);
            log.info("注册自定义工具: {}", def.name);
        }
    }

    /**
     * 清除所有注册的自定义工具
     */
    public void clearTools() {
        toolDefMap.clear();
    }

    /**
     * AI 调用自定义工具的入口
     * AI 通过 toolName 指定要调用的工具，params 为 JSON 格式的参数
     */
    @Tool("调用自定义工具。toolName 为工具名称，params 为 JSON 格式的参数（如 {\"text\":\"hello\",\"size\":\"200\"}）")
    public String callCustomTool(
            @P("工具名称") String toolName,
            @P("JSON 格式的参数") String params) {

        setupMonitorContext();

        CustomToolDefinition def = toolDefMap.get(toolName);
        if (def == null) {
            return "未知的自定义工具: " + toolName;
        }

        try {
            JSONObject argsObj = JSONUtil.parseObj(params != null ? params : "{}");

            // 替换 endpoint 中的参数占位符
            String url = def.endpoint;
            for (CustomToolParam param : def.parameters) {
                String value = argsObj.getStr(param.name, param.defaultValue);
                if (value != null) {
                    url = url.replace("{" + param.name + "}", URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            }

            // 执行 HTTP 请求
            HttpResponse response;
            if ("POST".equalsIgnoreCase(def.method)) {
                response = HttpRequest.post(url)
                        .header("Content-Type", "application/json")
                        .timeout(10000)
                        .execute();
            } else {
                response = HttpRequest.get(url)
                        .timeout(10000)
                        .execute();
            }

            if (response.isOk()) {
                String body = response.body();
                if (def.responsePath != null && !def.responsePath.isEmpty()) {
                    try {
                        JSONObject json = JSONUtil.parseObj(body);
                        Object extracted = json.getByPath(def.responsePath);
                        return extracted != null ? extracted.toString() : body;
                    } catch (Exception e) {
                        return body;
                    }
                }
                return body;
            } else {
                return "请求失败，状态码: " + response.getStatus();
            }
        } catch (Exception e) {
            log.error("自定义工具执行失败: toolName={}, error={}", toolName, e.getMessage());
            return "工具执行失败: " + e.getMessage();
        }
    }

    /**
     * 获取已注册的工具名称列表（用于日志）
     */
    public List<String> getRegisteredToolNames() {
        return new ArrayList<>(toolDefMap.keySet());
    }

    @Override
    public String getToolName() {
        return "callCustomTool";
    }

    @Override
    public String getDisplayName() {
        return "自定义工具";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        String toolName = arguments.getStr("toolName", "");
        return String.format("✅ 自定义工具 → `%s`", toolName);
    }

    // ==================== 内部数据结构 ====================

    private List<CustomToolDefinition> parseToolDefinitions(String json) {
        List<CustomToolDefinition> defs = new ArrayList<>();
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                CustomToolDefinition def = new CustomToolDefinition();
                def.name = obj.getStr("name");
                def.description = obj.getStr("description", "");
                def.endpoint = obj.getStr("endpoint");
                def.method = obj.getStr("method", "GET");
                def.responsePath = obj.getStr("responsePath", "");

                def.parameters = new ArrayList<>();
                JSONArray params = obj.getJSONArray("parameters");
                if (params != null) {
                    for (int j = 0; j < params.size(); j++) {
                        JSONObject p = params.getJSONObject(j);
                        CustomToolParam param = new CustomToolParam();
                        param.name = p.getStr("name");
                        param.type = p.getStr("type", "string");
                        param.description = p.getStr("description", "");
                        param.required = p.getBool("required", false);
                        param.defaultValue = p.getStr("default", "");
                        def.parameters.add(param);
                    }
                }

                if (def.name != null && def.endpoint != null) {
                    defs.add(def);
                }
            }
        } catch (Exception e) {
            log.error("解析自定义工具定义失败: {}", e.getMessage());
        }
        return defs;
    }

    static class CustomToolDefinition {
        String name;
        String description;
        String endpoint;
        String method;
        String responsePath;
        List<CustomToolParam> parameters;
    }

    static class CustomToolParam {
        String name;
        String type;
        String description;
        boolean required;
        String defaultValue;
    }
}
