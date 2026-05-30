package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 配色方案生成工具
 * 支持 AI 通过工具调用的方式生成和谐的配色方案
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class ColorPaletteTool extends BaseTool {

    @Tool("根据关键词生成配色方案。style 可选: modern(现代), warm(暖色), cool(冷色), pastel(柔和), dark(深色), vibrant(鲜艳)。返回5个主色调的 HEX 值和用途建议。")
    public String generateColorPalette(
            @P("配色风格关键词") String style,
            @P("应用场景描述，如：电商网站、个人博客、企业官网") String context,
            @ToolMemoryId Long appId
    ) {
        try {
            String palette;
            switch (style.toLowerCase()) {
                case "modern":
                case "现代":
                    palette = "现代风格配色方案：\n" +
                            "- 主色: #2563EB (蓝色，用于按钮、链接、重点元素)\n" +
                            "- 辅色: #7C3AED (紫色，用于高亮、徽章)\n" +
                            "- 背景: #F8FAFC (浅灰白，页面背景)\n" +
                            "- 文字: #1E293B (深灰蓝，正文文字)\n" +
                            "- 强调: #F59E0B (琥珀色，CTA按钮、重要提示)";
                    break;
                case "warm":
                case "暖色":
                    palette = "暖色风格配色方案：\n" +
                            "- 主色: #EA580C (橙色，用于按钮、品牌标识)\n" +
                            "- 辅色: #DC2626 (红色，用于促销、紧急提示)\n" +
                            "- 背景: #FFF7ED (浅橙白，页面背景)\n" +
                            "- 文字: #431407 (深棕，正文文字)\n" +
                            "- 强调: #F59E0B (金色，价格、优惠信息)";
                    break;
                case "cool":
                case "冷色":
                    palette = "冷色风格配色方案：\n" +
                            "- 主色: #0891B2 (青色，用于按钮、导航)\n" +
                            "- 辅色: #6366F1 (靛蓝，用于卡片、标签)\n" +
                            "- 背景: #F0F9FF (浅蓝白，页面背景)\n" +
                            "- 文字: #0C4A6E (深蓝，正文文字)\n" +
                            "- 强调: #10B981 (翠绿，成功状态、正面信息)";
                    break;
                case "pastel":
                case "柔和":
                    palette = "柔和风格配色方案：\n" +
                            "- 主色: #8B5CF6 (柔和紫，用于按钮、标题)\n" +
                            "- 辅色: #EC4899 (粉红，用于装饰、图标)\n" +
                            "- 背景: #FDF4FF (浅紫白，页面背景)\n" +
                            "- 文字: #581C87 (深紫，正文文字)\n" +
                            "- 强调: #06B6D4 (天蓝，链接、交互元素)";
                    break;
                case "dark":
                case "深色":
                    palette = "深色风格配色方案：\n" +
                            "- 主色: #3B82F6 (亮蓝，用于按钮、链接)\n" +
                            "- 辅色: #8B5CF6 (亮紫，用于高亮)\n" +
                            "- 背景: #0F172A (深蓝黑，页面背景)\n" +
                            "- 文字: #E2E8F0 (浅灰，正文文字)\n" +
                            "- 强调: #F97316 (橙色，CTA按钮、重要元素)";
                    break;
                case "vibrant":
                case "鲜艳":
                    palette = "鲜艳风格配色方案：\n" +
                            "- 主色: #7C3AED (紫色，用于品牌、按钮)\n" +
                            "- 辅色: #EC4899 (粉色，用于促销、活动)\n" +
                            "- 背景: #FFFFFF (纯白，页面背景)\n" +
                            "- 文字: #111827 (近黑，正文文字)\n" +
                            "- 强调: #F59E0B (金色，限时优惠、热门标签)";
                    break;
                default:
                    palette = "默认配色方案（专业商务风格）：\n" +
                            "- 主色: #2563EB (蓝色，用于按钮、导航、品牌)\n" +
                            "- 辅色: #475569 (灰色，用于次要元素)\n" +
                            "- 背景: #F1F5F9 (浅灰，页面背景)\n" +
                            "- 文字: #1E293B (深灰，正文文字)\n" +
                            "- 强调: #059669 (绿色，成功状态、正面操作)";
                    break;
            }

            String result = String.format("为「%s」场景生成的配色方案：\n\n%s\n使用建议：\n- 主色用于品牌标识、主要按钮、导航栏\n- 辅色用于次要按钮、标签、卡片边框\n- 背景色保持浅淡，不要喧宾夺主\n- 文字色与背景对比度至少 4.5:1（WCAG AA标准）\n- 强调色仅用于需要用户注意的关键元素", context, palette);

            log.info("配色方案生成: style={}, context={}", style, context);
            return result;

        } catch (Exception e) {
            log.error("配色方案生成失败: style={}, error={}", style, e.getMessage());
            return "生成失败: " + e.getMessage();
        }
    }

    @Override
    public String getToolName() {
        return "generateColorPalette";
    }

    @Override
    public String getDisplayName() {
        return "生成配色方案";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("✅ %s", getDisplayName());
    }
}
