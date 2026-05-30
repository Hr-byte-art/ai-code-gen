package com.wjh.aicodegen.skill;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Skill 生命周期钩子执行器
 * 支持 HTTP 类型的钩子：beforeGenerate / afterGenerate
 *
 * 钩子定义格式（JSON）：
 * {
 *   "beforeGenerate": [
 *     {"type": "http", "url": "https://api.example.com/before", "method": "POST"}
 *   ],
 *   "afterGenerate": [
 *     {"type": "http", "url": "https://api.example.com/after", "method": "POST"}
 *   ]
 * }
 */
@Slf4j
@Component
public class SkillHookExecutor {

    private static final int HOOK_TIMEOUT_MS = 10000;

    /**
     * 执行 beforeGenerate 钩子
     *
     * @param hooksJson 钩子定义 JSON
     * @param context   上下文数据（userMessage, appId 等）
     * @return 钩子返回的额外数据（可能为 null）
     */
    public JSONObject executeBeforeGenerate(String hooksJson, Map<String, Object> context) {
        return executeHooks(hooksJson, "beforeGenerate", context);
    }

    /**
     * 执行 afterGenerate 钩子
     *
     * @param hooksJson   钩子定义 JSON
     * @param context     上下文数据
     * @param generatedCode 生成的代码
     * @return 钩子返回的额外数据（可能为 null）
     */
    public JSONObject executeAfterGenerate(String hooksJson, Map<String, Object> context, String generatedCode) {
        Map<String, Object> ctx = new java.util.HashMap<>(context);
        ctx.put("generatedCode", generatedCode);
        return executeHooks(hooksJson, "afterGenerate", ctx);
    }

    /**
     * 执行指定阶段的钩子
     */
    private JSONObject executeHooks(String hooksJson, String phase, Map<String, Object> context) {
        if (hooksJson == null || hooksJson.isBlank()) return null;

        try {
            JSONObject hooks = JSONUtil.parseObj(hooksJson);
            Object phaseHooks = hooks.get(phase);
            if (phaseHooks == null) return null;

            // 支持单个钩子或钩子数组
            java.util.List<Object> hookList;
            if (phaseHooks instanceof java.util.List) {
                hookList = (java.util.List<Object>) phaseHooks;
            } else {
                hookList = java.util.List.of(phaseHooks);
            }

            JSONObject lastResult = null;
            for (Object hookObj : hookList) {
                if (hookObj instanceof Map) {
                    Map<String, Object> hook = (Map<String, Object>) hookObj;
                    String type = String.valueOf(hook.getOrDefault("type", "http"));

                    if ("http".equals(type)) {
                        lastResult = executeHttpHook(hook, context);
                    } else {
                        log.warn("不支持的钩子类型: {}", type);
                    }
                }
            }

            return lastResult;
        } catch (Exception e) {
            log.error("执行钩子异常: phase={}, error={}", phase, e.getMessage());
            return null;
        }
    }

    /**
     * 执行 HTTP 钩子
     */
    private JSONObject executeHttpHook(Map<String, Object> hook, Map<String, Object> context) {
        String url = String.valueOf(hook.get("url"));
        String method = String.valueOf(hook.getOrDefault("method", "POST")).toUpperCase();

        if (url == null || url.isBlank()) {
            log.warn("钩子 URL 为空，跳过");
            return null;
        }

        // 替换 URL 中的占位符
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            url = url.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        try {
            // 构建请求体
            JSONObject body = new JSONObject();
            body.putAll(context);

            HttpResponse response;
            if ("POST".equals(method)) {
                response = HttpRequest.post(url)
                        .header("Content-Type", "application/json")
                        .timeout(HOOK_TIMEOUT_MS)
                        .body(body.toString())
                        .execute();
            } else {
                response = HttpRequest.get(url)
                        .timeout(HOOK_TIMEOUT_MS)
                        .execute();
            }

            if (response.isOk()) {
                String responseBody = response.body();
                log.info("钩子执行成功: url={}, status={}", url, response.getStatus());
                try {
                    return JSONUtil.parseObj(responseBody);
                } catch (Exception e) {
                    // 响应不是 JSON，返回包含原始响应的对象
                    JSONObject result = new JSONObject();
                    result.put("response", responseBody);
                    return result;
                }
            } else {
                log.warn("钩子执行失败: url={}, status={}", url, response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("钩子 HTTP 请求异常: url={}, error={}", url, e.getMessage());
            return null;
        }
    }
}
