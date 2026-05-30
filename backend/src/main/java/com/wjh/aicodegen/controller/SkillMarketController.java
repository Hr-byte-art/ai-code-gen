package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.service.SkillMarketService;
import com.wjh.aicodegen.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Skill 市场接口
 */
@RestController
@RequestMapping("/skill/market")
public class SkillMarketController {

    @Resource
    private SkillMarketService skillMarketService;

    @Resource
    private UserService userService;

    /**
     * 获取公开 Skill 列表
     */
    @GetMapping("/list")
    public BaseResponse<List<CodeSkill>> listPublicSkills() {
        List<CodeSkill> skills = skillMarketService.listPublicSkills();
        // 隐藏 systemPrompt
        skills.forEach(s -> s.setSystemPrompt(null));
        return ResultUtils.success(skills);
    }

    /**
     * 发布 Skill 到市场
     */
    @PostMapping("/publish")
    @AuthCheck
    public BaseResponse<Boolean> publish(@RequestParam Long skillId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        skillMarketService.publish(skillId, user.getId());
        return ResultUtils.success(true);
    }

    /**
     * 取消发布
     */
    @PostMapping("/unpublish")
    @AuthCheck
    public BaseResponse<Boolean> unpublish(@RequestParam Long skillId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        skillMarketService.unpublish(skillId, user.getId());
        return ResultUtils.success(true);
    }

    /**
     * 安装 Skill
     */
    @PostMapping("/install")
    @AuthCheck
    public BaseResponse<Boolean> install(@RequestParam Long skillId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        skillMarketService.install(skillId, user.getId());
        return ResultUtils.success(true);
    }

    /**
     * 卸载 Skill
     */
    @PostMapping("/uninstall")
    @AuthCheck
    public BaseResponse<Boolean> uninstall(@RequestParam Long skillId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        skillMarketService.uninstall(skillId, user.getId());
        return ResultUtils.success(true);
    }

    /**
     * 评分
     */
    @PostMapping("/rate")
    @AuthCheck
    public BaseResponse<Boolean> rate(@RequestBody RateRequest req, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        skillMarketService.rate(req.getSkillId(), user.getId(), req.getScore(), req.getComment());
        return ResultUtils.success(true);
    }

    /**
     * 获取用户已安装的 Skill
     */
    @GetMapping("/installed")
    @AuthCheck
    public BaseResponse<List<CodeSkill>> getInstalled(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        List<CodeSkill> skills = skillMarketService.getInstalledSkills(user.getId());
        skills.forEach(s -> s.setSystemPrompt(null));
        return ResultUtils.success(skills);
    }

    /**
     * 检查是否已安装
     */
    @GetMapping("/installed/check")
    @AuthCheck
    public BaseResponse<Boolean> isInstalled(@RequestParam Long skillId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(skillMarketService.isInstalled(skillId, user.getId()));
    }

    @Data
    static class RateRequest {
        private Long skillId;
        private Integer score;
        private String comment;
    }
}
