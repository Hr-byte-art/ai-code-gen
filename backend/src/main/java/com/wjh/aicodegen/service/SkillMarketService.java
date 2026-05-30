package com.wjh.aicodegen.service;

import com.wjh.aicodegen.model.entity.CodeSkill;

import java.util.List;

/**
 * Skill 市场服务
 */
public interface SkillMarketService {

    /**
     * 发布 Skill 到市场
     */
    void publish(Long skillId, Long userId);

    /**
     * 取消发布
     */
    void unpublish(Long skillId, Long userId);

    /**
     * 安装 Skill
     */
    void install(Long skillId, Long userId);

    /**
     * 卸载 Skill
     */
    void uninstall(Long skillId, Long userId);

    /**
     * 评分
     */
    void rate(Long skillId, Long userId, int score, String comment);

    /**
     * 获取公开 Skill 列表
     */
    List<CodeSkill> listPublicSkills();

    /**
     * 获取用户已安装的 Skill
     */
    List<CodeSkill> getInstalledSkills(Long userId);

    /**
     * 检查用户是否已安装某 Skill
     */
    boolean isInstalled(Long skillId, Long userId);
}
