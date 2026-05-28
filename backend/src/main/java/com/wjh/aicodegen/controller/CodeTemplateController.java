package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.entity.CodeTemplate;
import com.wjh.aicodegen.service.CodeTemplateService;
import com.wjh.aicodegen.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码模板接口
 *
 * @author 王哈哈
 */
@Slf4j
@RestController
@RequestMapping("/template")
@Tag(name = "代码模板接口", description = "代码模板相关接口")
public class CodeTemplateController {

    @Resource
    private CodeTemplateService codeTemplateService;

    /**
     * 获取所有可用模板
     */
    @GetMapping("/list")
    @Operation(summary = "获取模板列表", description = "获取所有可用的代码模板")
    public BaseResponse<List<CodeTemplate>> listTemplates() {
        try {
            List<CodeTemplate> templates = codeTemplateService.listActiveTemplates();
            return ResultUtils.success(templates);
        } catch (Exception e) {
            log.error("获取模板列表失败: {}", e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取模板列表失败");
        }
    }

    /**
     * 根据标识获取模板
     */
    @GetMapping("/get")
    @Operation(summary = "获取模板详情", description = "根据模板标识获取模板详情")
    public BaseResponse<CodeTemplate> getTemplate(
            @Parameter(description = "模板标识") @RequestParam String key) {
        try {
            CodeTemplate template = codeTemplateService.getByKey(key);
            if (template == null) {
                return new BaseResponse<>(ErrorCode.NOT_FOUND_ERROR.getCode(), null, "模板不存在");
            }
            return ResultUtils.success(template);
        } catch (Exception e) {
            log.error("获取模板详情失败: key={}, error={}", key, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取模板详情失败");
        }
    }
}
