package com.wjh.aicodegen.ai.tools;

import cn.hutool.json.JSONObject;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.service.CodeSkillService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 技能加载工具
 * 让 AI 自主查看和选择代码生成技能，而不是通过独立的路由服务
 *
 * 设计思路（参考 OpenClaw）：
 * - 系统 prompt 告诉 AI "你有 loadSkill 工具可以加载技能"
 * - AI 根据用户需求自主决定加载哪个技能
 * - 加载后，技能的 system prompt 成为 AI 的行为指令
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class SkillLoadTool extends BaseTool {

    @Resource
    private CodeSkillService codeSkillService;

    @Tool("列出所有可用的代码生成技能。在生成代码前调用此工具查看有哪些技能可选。")
    public String listAvailableSkills(@ToolMemoryId Long appId) {
        List<CodeSkill> skills = codeSkillService.listActiveSkills();
        if (skills.isEmpty()) {
            return "当前没有可用的技能";
        }

        StringBuilder sb = new StringBuilder("可用的代码生成技能：\n\n");
        for (CodeSkill s : skills) {
            sb.append(String.format("- [%s] %s: %s (积分: %d)\n",
                    s.getSkillKey(), s.getName(), s.getDescription(), s.getPointCost()));
        }
        sb.append("\n使用 loadSkill(skillKey) 加载具体技能的详细指令。");
        return sb.toString();
    }

    @Tool("加载指定的代码生成技能。加载后，你将获得该技能的详细指令，按照指令为用户生成代码。"
            + "在确定用户想要生成什么类型的代码后调用此工具。")
    public String loadSkill(
            @P("技能标识，如 html, vue_project, react_ts 等") String skillKey,
            @ToolMemoryId Long appId
    ) {
        CodeSkill skill = codeSkillService.getByKey(skillKey);
        if (skill == null) {
            // 尝试模糊匹配
            List<CodeSkill> all = codeSkillService.listActiveSkills();
            List<CodeSkill> matches = all.stream()
                    .filter(s -> s.getName().contains(skillKey) || s.getDescription().contains(skillKey))
                    .collect(Collectors.toList());
            if (matches.size() == 1) {
                skill = matches.get(0);
            } else if (matches.size() > 1) {
                StringBuilder sb = new StringBuilder("找到多个匹配的技能，请指定精确的 skillKey：\n");
                for (CodeSkill m : matches) {
                    sb.append(String.format("- %s (%s)\n", m.getSkillKey(), m.getName()));
                }
                return sb.toString();
            } else {
                return "未找到技能「" + skillKey + "」，请先调用 listAvailableSkills 查看可用技能列表。";
            }
        }

        log.info("AI 加载技能: key={}, name={}, appId={}", skill.getSkillKey(), skill.getName(), appId);

        // 返回技能的 system prompt 作为 AI 的行为指令
        return String.format(
                "=== 已加载技能: %s ===\n" +
                "技能标识: %s\n" +
                "描述: %s\n\n" +
                "请严格按照以下指令为用户生成代码：\n\n" +
                "---\n" +
                "%s\n" +
                "---\n\n" +
                "现在请根据用户的描述开始生成代码。使用 writeFile 工具写入文件。",
                skill.getName(), skill.getSkillKey(), skill.getDescription(),
                skill.getSystemPrompt());
    }

    @Override
    public String getToolName() {
        return "loadSkill";
    }

    @Override
    public String getDisplayName() {
        return "加载技能";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String skillKey = arguments.getStr("skillKey");
        if (skillKey != null) {
            return String.format("✅ %s → `%s`", getDisplayName(), skillKey);
        }
        return String.format("✅ %s", getDisplayName());
    }
}
