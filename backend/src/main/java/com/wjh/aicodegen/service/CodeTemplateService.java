package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.entity.CodeTemplate;

import java.util.List;

/**
 * 代码模板 服务层。
 *
 * @author 王哈哈
 */
public interface CodeTemplateService extends IService<CodeTemplate> {

    /**
     * 获取所有启用的模板列表
     *
     * @return 模板列表
     */
    List<CodeTemplate> listActiveTemplates();

    /**
     * 根据模板标识获取模板
     *
     * @param key 模板标识
     * @return 模板
     */
    CodeTemplate getByKey(String key);

    /**
     * 增加模板使用次数
     *
     * @param id 模板ID
     */
    void incrementUseCount(Long id);
}
