package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 颜色生成工具
 * 使用 xColors API（无需认证）
 */
@Slf4j
@Component
public class ColorGeneratorTool extends PublicApiTool {

    private static final String API = "https://x-colors.yurace.pro/api/random";

    @Tool("生成随机颜色或配色方案。返回 HEX 色值列表，可用于替代硬编码颜色。")
    public String generate(@P("需要的颜色数量，1-10") Integer count) {
        setupMonitorContext();

        int num = (count != null && count > 0) ? Math.min(count, 10) : 5;
        String url = API + "?number=" + num;

        String responseBody = doGetWithCache(url);
        if (responseBody == null) {
            return "颜色生成失败，请稍后重试";
        }

        try {
            JSONArray colors = JSONUtil.parseArray(responseBody);
            StringBuilder sb = new StringBuilder();
            sb.append("配色方案（").append(colors.size()).append(" 色）:\n\n");
            for (int i = 0; i < colors.size(); i++) {
                JSONObject color = colors.getJSONObject(i);
                String hex = color.getStr("hex", "#000000");
                sb.append(i + 1).append(". ").append(hex).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("解析颜色响应失败: {}", e.getMessage());
            return "解析颜色数据失败";
        }
    }

    @Override
    public String getToolName() {
        return "colorGenerator";
    }

    @Override
    public String getDisplayName() {
        return "颜色生成";
    }

    @Override
    public String generateToolExecutedResult(cn.hutool.json.JSONObject arguments) {
        return String.format("✅ %s", getDisplayName());
    }
}
