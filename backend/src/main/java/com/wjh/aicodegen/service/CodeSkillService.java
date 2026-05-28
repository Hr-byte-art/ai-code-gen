package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.entity.CodeSkill;

import java.util.List;

/**
 * 代码生成技能 服务层。
 *
 * @author 王哈哈
 */
public interface CodeSkillService extends IService<CodeSkill> {

    /**
     * 获取所有启用的技能列表
     */
    List<CodeSkill> listActiveSkills();

    /**
     * 根据 skill_key 获取技能
     */
    CodeSkill getByKey(String skillKey);

    /**
     * 根据 code_gen_type 获取技能
     */
    CodeSkill getByCodeGenType(String codeGenType);

    /**
     * 根据 code_gen_type 获取技能（带缓存）
     */
    CodeSkill getByCodeGenTypeCached(String codeGenType);

    /**
     * 清除缓存
     */
    void clearCache();
}
