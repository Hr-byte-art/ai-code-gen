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

import java.time.LocalDateTime;

/**
 * 技能创建工具
 * 允许 AI 根据用户需求动态创建新的代码生成技能
 *
 * @author 王哈哈
 */
@Slf4j
@Component
public class SkillCreateTool extends BaseTool {

    @Resource
    private CodeSkillService codeSkillService;

    @Tool("创建一个新的代码生成技能。当用户需要一种新的代码生成能力时（如React项目、Node.js API、H5页面等），使用此工具创建技能。"
            + "创建后用户即可使用该技能生成代码。")
    public String createSkill(
            @P("技能名称，如：React 项目生成") String name,
            @P("唯一标识，如：react_project（小写下划线）") String skillKey,
            @P("技能描述，说明这个技能能生成什么") String description,
            @P("系统提示词，指导 AI 如何生成代码。应包含：技术栈、项目结构、输出格式、质量要求等") String systemPrompt,
            @P("代码生成类型标识，如：react_project") String codeGenType,
            @P("积分消耗，简单类型5-10，复杂类型15-25") Integer pointCost,
            @P("构建策略：none（无需构建）/vue（Vue构建）/fullstack（全栈构建）") String buildStrategy,
            @P("模型策略：standard（标准模型）/reasoning（推理模型，适合复杂生成）") String modelStrategy,
            @ToolMemoryId Long appId
    ) {
        try {
            // 校验 skillKey 是否已存在
            CodeSkill existing = codeSkillService.getByKey(skillKey);
            if (existing != null) {
                return "错误：skill_key 「" + skillKey + "」已存在，请使用不同的标识";
            }

            // 校验必填字段
            if (name == null || name.isBlank()) return "错误：技能名称不能为空";
            if (skillKey == null || skillKey.isBlank()) return "错误：唯一标识不能为空";
            if (systemPrompt == null || systemPrompt.isBlank()) return "错误：系统提示词不能为空";
            if (codeGenType == null || codeGenType.isBlank()) return "错误：代码生成类型不能为空";

            // 设置默认值
            if (description == null || description.isBlank()) description = name;
            if (pointCost == null || pointCost <= 0) pointCost = 10;
            if (buildStrategy == null || buildStrategy.isBlank()) buildStrategy = "none";
            if (modelStrategy == null || modelStrategy.isBlank()) modelStrategy = "standard";

            CodeSkill skill = CodeSkill.builder()
                    .name(name)
                    .skillKey(skillKey)
                    .description(description)
                    .systemPrompt(systemPrompt)
                    .codeGenType(codeGenType)
                    .pointCost(pointCost)
                    .toolNames(null) // 默认使用全部工具
                    .buildStrategy(buildStrategy)
                    .modelStrategy(modelStrategy)
                    .isActive(1)
                    .sortOrder(50)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();

            codeSkillService.save(skill);

            String result = String.format("""
                    技能创建成功！
                    - 名称: %s
                    - 标识: %s
                    - 类型: %s
                    - 积分: %d
                    - 构建: %s
                    - 模型: %s

                    用户现在可以通过「%s」来生成代码了。""",
                    name, skillKey, codeGenType, pointCost, buildStrategy, modelStrategy, name);

            log.info("AI 创建新技能: key={}, name={}, type={}", skillKey, name, codeGenType);
            return result;

        } catch (Exception e) {
            log.error("创建技能失败: name={}, error={}", name, e.getMessage());
            return "创建技能失败: " + e.getMessage();
        }
    }

    @Override
    public String getToolName() {
        return "createSkill";
    }

    @Override
    public String getDisplayName() {
        return "创建技能";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String name = arguments.getStr("name");
        return String.format("✅ %s → `%s`", getDisplayName(), name);
    }
}
