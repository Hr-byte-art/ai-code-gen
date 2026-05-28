package com.wjh.aicodegen.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.mapper.CodeTemplateMapper;
import com.wjh.aicodegen.model.entity.CodeTemplate;
import com.wjh.aicodegen.service.CodeTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 代码模板 服务实现。
 *
 * @author 王哈哈
 */
@Service
@Slf4j
public class CodeTemplateServiceImpl extends ServiceImpl<CodeTemplateMapper, CodeTemplate> implements CodeTemplateService {

    @Override
    public List<CodeTemplate> listActiveTemplates() {
        return list(QueryWrapper.create()
                .where("status = ?", 1)
                .orderBy("use_count", false));
    }

    @Override
    public CodeTemplate getByKey(String key) {
        return getOne(QueryWrapper.create()
                .where("template_key = ?", key)
                .and("status = ?", 1));
    }

    @Override
    public void incrementUseCount(Long id) {
        CodeTemplate template = getById(id);
        if (template != null) {
            template.setUseCount(template.getUseCount() + 1);
            updateById(template);
        }
    }
}
