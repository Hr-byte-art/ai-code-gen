package com.wjh.aicodegen.service;

import java.util.List;
import java.util.Map;

/**
 * 设计模板服务
 * 管理 DESIGN.md 模板，支持按风格注入到 system prompt
 */
public interface DesignTemplateService {

    /**
     * 获取所有可用的设计模板列表
     */
    List<DesignTemplateInfo> listAvailableTemplates();

    /**
     * 根据 key 获取设计模板内容
     */
    String getDesignTemplate(String key);

    /**
     * 将设计模板注入到 system prompt 中
     */
    String injectDesignPrompt(String systemPrompt, String designKey);

    /**
     * 设计模板基本信息
     */
    record DesignTemplateInfo(
        String key,
        String name,
        String description
    ) {}
}
