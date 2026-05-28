package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.vo.UserQuotaVO;
import com.wjh.aicodegen.service.UserQuotaService;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户配额接口
 *
 * @author 王哈哈
 */
@Slf4j
@RestController
@RequestMapping("/quota")
@Tag(name = "用户配额接口", description = "用户用量配额相关接口")
public class UserQuotaController {

    @Resource
    private UserQuotaService userQuotaService;

    @Resource
    private UserService userService;

    /**
     * 获取当前用户的配额使用情况
     */
    @GetMapping("/my")
    @Operation(summary = "获取当前用户配额", description = "查看当前用户的生成次数和Token消耗配额")
    public BaseResponse<UserQuotaVO> getMyQuota(HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);
            UserQuotaVO quotaVO = userQuotaService.getUserQuotaVO(loginUser.getId(), loginUser.getUserRole());
            return ResultUtils.success(quotaVO);
        } catch (Exception e) {
            log.error("获取用户配额失败: {}", e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取配额失败");
        }
    }
}
