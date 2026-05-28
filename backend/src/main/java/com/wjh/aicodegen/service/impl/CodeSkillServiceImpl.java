package com.wjh.aicodegen.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.CodeSkillMapper;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.service.CodeSkillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 代码生成技能 服务实现。
 *
 * @author 王哈哈
 */
@Service
@Slf4j
public class CodeSkillServiceImpl extends ServiceImpl<CodeSkillMapper, CodeSkill> implements CodeSkillService {

    /**
     * 缓存：codeGenType -> CodeSkill
     */
    private final Cache<String, CodeSkill> cache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    @Override
    public List<CodeSkill> listActiveSkills() {
        return list(QueryWrapper.create()
                .where("is_active = ?", 1)
                .orderBy("sort_order", true));
    }

    @Override
    public CodeSkill getByKey(String skillKey) {
        return getOne(QueryWrapper.create()
                .where("skill_key = ?", skillKey)
                .and("is_active = ?", 1));
    }

    @Override
    public CodeSkill getByCodeGenType(String codeGenType) {
        return getOne(QueryWrapper.create()
                .where("code_gen_type = ?", codeGenType)
                .and("is_active = ?", 1));
    }

    @Override
    public CodeSkill getByCodeGenTypeCached(String codeGenType) {
        return cache.get(codeGenType, this::getByCodeGenType);
    }

    @Override
    public void clearCache() {
        cache.invalidateAll();
        log.info("Skill 缓存已清除");
    }
}
