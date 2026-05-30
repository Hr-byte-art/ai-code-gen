package com.wjh.aicodegen.skill;

import com.wjh.aicodegen.model.entity.CodeSkill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

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
    @SuppressWarnings("unchecked")
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

            // 使用 snakeyaml 解析 YAML（支持 JSON 值）
            Yaml yaml = new Yaml();
            Map<String, Object> meta = yaml.load(parts[0]);
            if (meta == null) meta = new LinkedHashMap<>();

            String systemPrompt = parts[1].trim();

            if (!meta.containsKey("skillKey") || !meta.containsKey("name")) {
                log.warn("SKILL.md 缺少必填字段 (skillKey, name): {}", resource.getFilename());
                return null;
            }

            String skillKey = String.valueOf(meta.get("skillKey")).trim();

            // customTools 可能是 List 或 String，统一转为 JSON 字符串
            String customToolsJson = null;
            Object customToolsObj = meta.get("customTools");
            if (customToolsObj != null) {
                if (customToolsObj instanceof List) {
                    customToolsJson = cn.hutool.json.JSONUtil.toJsonStr(customToolsObj);
                } else {
                    customToolsJson = String.valueOf(customToolsObj).trim();
                }
            }

            // hooks 可能是 Map 或 String，统一转为 JSON 字符串
            String hooksJson = null;
            Object hooksObj = meta.get("hooks");
            if (hooksObj != null) {
                if (hooksObj instanceof Map) {
                    hooksJson = cn.hutool.json.JSONUtil.toJsonStr(hooksObj);
                } else {
                    hooksJson = String.valueOf(hooksObj).trim();
                }
            }

            return CodeSkill.builder()
                    .skillKey(skillKey)
                    .name(getString(meta, "name", skillKey))
                    .description(getString(meta, "description", ""))
                    .codeGenType(getString(meta, "codeGenType", skillKey))
                    .pointCost(getInt(meta, "pointCost", 10))
                    .toolNames(getString(meta, "toolNames", ""))
                    .buildStrategy(getString(meta, "buildStrategy", "none"))
                    .modelStrategy(getString(meta, "modelStrategy", "reasoning"))
                    .sortOrder(getInt(meta, "sortOrder", 99))
                    .isActive(getBool(meta, "isActive", true) ? 1 : 0)
                    .customTools(customToolsJson)
                    .hooks(hooksJson)
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

    private String getString(Map<String, Object> meta, String key, String defaultValue) {
        Object val = meta.get(key);
        return val != null ? String.valueOf(val).trim() : defaultValue;
    }

    private int getInt(Map<String, Object> meta, String key, int defaultValue) {
        Object val = meta.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val).trim()); } catch (Exception e) { return defaultValue; }
    }

    private boolean getBool(Map<String, Object> meta, String key, boolean defaultValue) {
        Object val = meta.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val == null) return defaultValue;
        String s = String.valueOf(val).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
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
