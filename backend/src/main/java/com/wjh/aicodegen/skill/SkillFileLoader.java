package com.wjh.aicodegen.skill;

import com.wjh.aicodegen.model.entity.CodeSkill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 classpath 加载 skills/SKILL.md 文件，解析为 CodeSkill 列表
 */
@Slf4j
@Component
public class SkillFileLoader {

    private static final String SKILLS_PATTERN = "classpath:skills/*/SKILL.md";

    /**
     * 扫描并解析所有 SKILL.md 文件
     */
    public List<CodeSkill> loadAll() {
        List<CodeSkill> skills = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SKILLS_PATTERN);

            for (Resource resource : resources) {
                try {
                    CodeSkill skill = parse(resource);
                    if (skill != null) {
                        skills.add(skill);
                    }
                } catch (Exception e) {
                    log.error("解析 SKILL.md 失败: {}, error: {}", resource.getFilename(), e.getMessage());
                }
            }

            log.info("从文件加载了 {} 个 Skill", skills.size());
        } catch (Exception e) {
            log.error("扫描 skills 目录失败: {}", e.getMessage());
        }
        return skills;
    }

    /**
     * 解析单个 SKILL.md 文件
     * 格式: YAML frontmatter (--- 之间) + Markdown body (--- 之后)
     */
    public CodeSkill parse(Resource resource) {
        try {
            String content = readResource(resource);
            if (content == null || content.isBlank()) {
                log.warn("SKILL.md 文件为空: {}", resource.getFilename());
                return null;
            }

            // 分割 frontmatter 和 body
            String[] parts = splitFrontmatter(content);
            if (parts == null) {
                log.warn("SKILL.md 格式错误，缺少 frontmatter: {}", resource.getFilename());
                return null;
            }

            Map<String, String> meta = parseYamlSimple(parts[0]);
            String systemPrompt = parts[1].trim();

            if (!meta.containsKey("skillKey") || !meta.containsKey("name")) {
                log.warn("SKILL.md 缺少必填字段 (skillKey, name): {}", resource.getFilename());
                return null;
            }

            String skillKey = meta.get("skillKey").trim();

            return CodeSkill.builder()
                    .skillKey(skillKey)
                    .name(meta.getOrDefault("name", skillKey).trim())
                    .description(meta.getOrDefault("description", "").trim())
                    .codeGenType(meta.getOrDefault("codeGenType", skillKey).trim())
                    .pointCost(parseInt(meta.getOrDefault("pointCost", "10")))
                    .toolNames(meta.getOrDefault("toolNames", "").trim())
                    .buildStrategy(meta.getOrDefault("buildStrategy", "none").trim())
                    .modelStrategy(meta.getOrDefault("modelStrategy", "reasoning").trim())
                    .sortOrder(parseInt(meta.getOrDefault("sortOrder", "99")))
                    .isActive(parseBoolean(meta.getOrDefault("isActive", "true")) ? 1 : 0)
                    .systemPrompt(systemPrompt)
                    .contentHash(md5(systemPrompt))
                    .source("file")
                    .isDelete(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("解析 SKILL.md 异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 分割 YAML frontmatter 和 Markdown body
     * 格式: ---\nYAML\n---\nBody
     */
    private String[] splitFrontmatter(String content) {
        content = content.stripLeading();
        if (!content.startsWith("---")) {
            return null;
        }

        int secondDash = content.indexOf("---", 3);
        if (secondDash < 0) {
            return null;
        }

        String yaml = content.substring(3, secondDash).trim();
        String body = content.substring(secondDash + 3).trim();

        if (yaml.isEmpty() || body.isEmpty()) {
            return null;
        }

        return new String[]{yaml, body};
    }

    /**
     * 简单的 YAML 解析（只支持单层 key: value）
     */
    private Map<String, String> parseYamlSimple(String yaml) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : yaml.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) continue;

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            // 去除引号
            if (value.length() >= 2 &&
                    ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }

            map.put(key, value);
        }
        return map;
    }

    private String readResource(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("读取资源文件失败: {}", e.getMessage());
            return null;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
