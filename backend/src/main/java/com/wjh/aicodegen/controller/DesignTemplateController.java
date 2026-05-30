package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.service.DesignTemplateService;
import com.wjh.aicodegen.service.DesignTemplateService.DesignTemplateInfo;
import com.wjh.aicodegen.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;

/**
 * 设计模板控制器
 */
@RestController
@RequestMapping("/design")
@Tag(name = "设计模板接口")
public class DesignTemplateController {

    @Resource
    private DesignTemplateService designTemplateService;

    @GetMapping("/list")
    @Operation(summary = "获取所有设计模板列表")
    public BaseResponse<List<DesignTemplateInfo>> listDesignTemplates() {
        return ResultUtils.success(designTemplateService.listAvailableTemplates());
    }

    @GetMapping("/get")
    @Operation(summary = "获取设计模板内容")
    public BaseResponse<String> getDesignTemplate(@RequestParam String key) {
        String content = designTemplateService.getDesignTemplate(key);
        if (content == null) {
            return null;
        }
        return ResultUtils.success(content);
    }
}
