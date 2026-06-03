package com.wjh.aicodegen.service.impl;

import com.wjh.aicodegen.service.DesignTemplateService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 设计模板服务实现
 * 从文件系统加载 DESIGN.md 模板
 */
@Slf4j
@Service
public class DesignTemplateServiceImpl implements DesignTemplateService {

    @Value("${design.template.dir:design-templates/design-md}")
    private String templateDir;

    private static final List<String> COMMON_TEMPLATE_KEYS = List.of(
        "apple",
        "stripe",
        "notion",
        "linear.app",
        "vercel",
        "figma",
        "airbnb",
        "shopify",
        "intercom"
    );

    private static final Set<String> COMMON_TEMPLATE_KEY_SET = new HashSet<>(COMMON_TEMPLATE_KEYS);

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadTemplates();
    }

    private void loadTemplates() {
        try {
            Path basePath = Paths.get(templateDir);
            if (!Files.exists(basePath)) {
                log.warn("设计模板目录不存在: {}", templateDir);
                return;
            }

            Files.list(basePath)
                .filter(Files::isDirectory)
                .filter(dir -> COMMON_TEMPLATE_KEY_SET.contains(dir.getFileName().toString()))
                .forEach(dir -> {
                    String key = dir.getFileName().toString();
                    Path designFile = dir.resolve("DESIGN.md");
                    if (Files.exists(designFile)) {
                        try {
                            String content = Files.readString(designFile, StandardCharsets.UTF_8);
                            templateCache.put(key, content);
                            log.debug("加载设计模板: {}", key);
                        } catch (IOException e) {
                            log.warn("读取设计模板失败: {}", key, e);
                        }
                    }
                });

            log.info("加载了 {} 个设计模板", templateCache.size());
        } catch (IOException e) {
            log.error("扫描设计模板目录失败", e);
        }
    }

    @Override
    public List<DesignTemplateInfo> listAvailableTemplates() {
        return COMMON_TEMPLATE_KEYS.stream()
            .filter(templateCache::containsKey)
            .map(key -> {
                String content = templateCache.get(key);
                String name = extractName(key, content);
                String description = extractDescription(content);
                return new DesignTemplateInfo(key, name, description);
            })
            .collect(Collectors.toList());
    }

    @Override
    public String getDesignTemplate(String key) {
        return templateCache.get(key);
    }

    @Override
    public String injectDesignPrompt(String systemPrompt, String designKey) {
        String designContent = templateCache.get(designKey);
        if (designContent == null) {
            log.warn("设计模板不存在: {}", designKey);
            return systemPrompt;
        }

        // 转义 DESIGN.md 中的 {{...}} 模板变量，防止 LangChain4j 误解析
        String escapedContent = designContent.replace("{{", "\\{\\{").replace("}}", "\\}\\}");

        return systemPrompt + "\n\n## 设计风格要求\n\n" +
            "请严格按照以下 DESIGN.md 的设计规范生成代码：\n\n" +
            escapedContent;
    }

    private String extractName(String key, String content) {
        // 将 key 转换为显示名称 (e.g., "stripe" -> "Stripe")
        String name = key.replace("-", " ");
        name = Arrays.stream(name.split(" "))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
        return name;
    }

    private String extractDescription(String content) {
        // 从 DESIGN.md 中提取第一段作为描述
        String[] lines = content.split("\n");
        StringBuilder desc = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (desc.length() > 0) break;
                continue;
            }
            if (trimmed.startsWith("#")) continue;
            if (trimmed.startsWith("```")) continue;
            if (trimmed.startsWith("|")) continue;
            if (trimmed.startsWith("-")) continue;
            desc.append(trimmed).append(" ");
            if (desc.length() > 200) break;
        }
        return desc.toString().trim();
    }
}
