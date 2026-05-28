package com.wjh.aicodegen.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.constant.UserConstant;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.service.CodeSkillService;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码生成技能接口
 *
 * @author 王哈哈
 */
@Slf4j
@RestController
@RequestMapping("/skill")
@Tag(name = "代码生成技能接口", description = "技能管理相关接口")
public class CodeSkillController {

    @Resource
    private CodeSkillService codeSkillService;

    /**
     * 获取所有可用技能（用户可见）
     */
    @GetMapping("/list")
    @Operation(summary = "获取技能列表", description = "获取所有启用的代码生成技能")
    public BaseResponse<List<CodeSkill>> listSkills() {
        List<CodeSkill> skills = codeSkillService.listActiveSkills();
        // 清除 system_prompt 字段，不暴露给用户
        skills.forEach(s -> s.setSystemPrompt(null));
        return ResultUtils.success(skills);
    }

    /**
     * 根据标识获取技能
     */
    @GetMapping("/get")
    @Operation(summary = "获取技能详情", description = "根据 skill_key 获取技能详情")
    public BaseResponse<CodeSkill> getSkill(@RequestParam String key) {
        CodeSkill skill = codeSkillService.getByKey(key);
        ThrowUtils.throwIf(skill == null, ErrorCode.NOT_FOUND_ERROR, "技能不存在");
        skill.setSystemPrompt(null);
        return ResultUtils.success(skill);
    }

    // ==================== 管理员接口 ====================

    /**
     * 获取所有技能（含禁用，管理员）
     */
    @GetMapping("/admin/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员获取所有技能")
    public BaseResponse<List<CodeSkill>> adminListSkills() {
        return ResultUtils.success(codeSkillService.list(QueryWrapper.create().orderBy("sort_order", true)));
    }

    /**
     * 新增技能（管理员）
     */
    @PostMapping("/admin/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "新增技能")
    public BaseResponse<Long> addSkill(@RequestBody CodeSkill skill) {
        ThrowUtils.throwIf(skill == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(skill.getSkillKey() == null, ErrorCode.PARAMS_ERROR, "skill_key 不能为空");
        codeSkillService.save(skill);
        return ResultUtils.success(skill.getId());
    }

    /**
     * 更新技能（管理员）
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "更新技能")
    public BaseResponse<Boolean> updateSkill(@RequestBody CodeSkill skill) {
        ThrowUtils.throwIf(skill == null || skill.getId() == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(codeSkillService.updateById(skill));
    }

    /**
     * 删除技能（管理员）
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除技能")
    public BaseResponse<Boolean> deleteSkill(@RequestParam Long id) {
        return ResultUtils.success(codeSkillService.removeById(id));
    }
}
