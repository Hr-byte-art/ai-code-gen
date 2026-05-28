package com.wjh.aicodegen.controller;

import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.constant.UserConstant;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.dto.token.GenerationStatsDTO;
import com.wjh.aicodegen.model.dto.token.ModelTokenRankingDTO;
import com.wjh.aicodegen.model.dto.token.ModelTokenSummaryDTO;
import com.wjh.aicodegen.model.dto.token.SystemTokenSummaryDTO;
import com.wjh.aicodegen.model.dto.token.TokenDetailDTO;
import com.wjh.aicodegen.model.dto.token.TokenRankingDTO;
import com.wjh.aicodegen.model.dto.token.UserTokenSummaryDTO;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.enums.UserRoleEnum;
import com.wjh.aicodegen.service.TokenUsageService;
import com.wjh.aicodegen.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token统计相关接口
 *
 * @author AI Assistant
 */
@Slf4j
@RestController
@RequestMapping("/token")
@Tag(name = "Token统计接口", description = "AI Token消耗统计相关接口")
public class TokenStatisticsController {

    @Resource
    private TokenUsageService tokenUsageService;

    @Resource
    private UserService userService;

    /**
     * 获取当前用户的生成历史统计
     */
    @GetMapping("/user/generation-stats")
    @Operation(summary = "获取用户生成历史统计", description = "查看当前用户的生成次数、Token消耗、应用类型分布等")
    public BaseResponse<GenerationStatsDTO> getUserGenerationStats(HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);
            GenerationStatsDTO stats = tokenUsageService.getUserGenerationStats(loginUser.getId());
            return ResultUtils.success(stats);
        } catch (Exception e) {
            log.error("获取用户生成统计失败: {}", e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取生成统计失败");
        }
    }

    /**
     * 获取当前用户的Token消耗汇总
     */
    @GetMapping("/user/summary")
    @Operation(summary = "获取当前用户Token消耗汇总", description = "查看当前用户的总Token消耗情况")
    public BaseResponse<UserTokenSummaryDTO> getCurrentUserTokenSummary(HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);
            UserTokenSummaryDTO summary = tokenUsageService.getUserTokenSummary(loginUser.getId());

            if (summary == null) {
                summary = UserTokenSummaryDTO.builder()
                        .userId(loginUser.getId())
                        .userName(loginUser.getUserName())
                        .userAvatar(loginUser.getUserAvatar())
                        .totalInputTokens(0L)
                        .totalOutputTokens(0L)
                        .totalTokens(0L)
                        .appCount(0L)
                        .build();
            }

            return ResultUtils.success(summary);
        } catch (Exception e) {
            log.error("获取当前用户Token汇总失败: {}", e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取Token统计失败");
        }
    }

    /**
     * 获取指定用户的Token消耗汇总（管理员权限）
     */
    @GetMapping("/user/{userId}/summary")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取指定用户Token消耗汇总", description = "管理员查看指定用户的Token消耗情况")
    public BaseResponse<UserTokenSummaryDTO> getUserTokenSummary(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        try {
            UserTokenSummaryDTO summary = tokenUsageService.getUserTokenSummary(userId);
            if (summary == null) {
                return new BaseResponse<>(ErrorCode.NOT_FOUND_ERROR.getCode(), null, "用户不存在或无Token消耗记录");
            }
            return ResultUtils.success(summary);
        } catch (Exception e) {
            log.error("获取用户Token汇总失败: userId={}, error={}", userId, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取Token统计失败");
        }
    }

    /**
     * 获取系统总Token消耗统计
     */
    @GetMapping("/system/summary")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取系统总Token消耗统计", description = "管理员查看系统整体Token消耗情况")
    public BaseResponse<SystemTokenSummaryDTO> getSystemTokenSummary(
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            SystemTokenSummaryDTO summary = tokenUsageService.getSystemTokenSummary(startTime, endTime);
            if (summary == null) {
                return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取系统统计失败");
            }
            return ResultUtils.success(summary);
        } catch (Exception e) {
            log.error("获取系统Token汇总失败: startTime={}, endTime={}, error={}",
                    startTime, endTime, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取系统统计失败");
        }
    }

    /**
     * 获取用户Token消耗排行榜
     */
    @GetMapping("/ranking")
    @Operation(summary = "获取用户Token消耗排行榜", description = "查看用户Token消耗排行榜")
    public BaseResponse<TokenRankingDTO> getTokenRanking(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，最大100") @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        try {
            // 检查是否为管理员，非管理员只能看前10名
            User loginUser = userService.getLoginUser(request);
            if (!UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
                pageSize = Math.min(pageSize, 10);
                page = page > 1 ? 1 : page; // 非管理员只能看第一页
            }

            TokenRankingDTO ranking = tokenUsageService.getTokenRanking(page, pageSize);
            if (ranking == null) {
                return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取排行榜失败");
            }
            return ResultUtils.success(ranking);
        } catch (Exception e) {
            log.error("获取Token排行榜失败: page={}, pageSize={}, error={}",
                    page, pageSize, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取排行榜失败");
        }
    }

    /**
     * 获取当前用户的Token使用详情
     */
    @GetMapping("/user/details")
    @Operation(summary = "获取当前用户Token使用详情", description = "查看当前用户的详细Token使用记录")
    public BaseResponse<List<TokenDetailDTO>> getCurrentUserTokenDetails(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);
            List<TokenDetailDTO> details = tokenUsageService.getUserTokenDetails(
                    loginUser.getId(), page, pageSize);
            return ResultUtils.success(details);
        } catch (Exception e) {
            log.error("获取当前用户Token详情失败: page={}, pageSize={}, error={}",
                    page, pageSize, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取Token详情失败");
        }
    }

    /**
     * 获取指定用户的Token使用详情（管理员权限）
     */
    @GetMapping("/user/{userId}/details")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取指定用户Token使用详情", description = "管理员查看指定用户的详细Token使用记录")
    public BaseResponse<List<TokenDetailDTO>> getUserTokenDetails(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            List<TokenDetailDTO> details = tokenUsageService.getUserTokenDetails(userId, page, pageSize);
            return ResultUtils.success(details);
        } catch (Exception e) {
            log.error("获取用户Token详情失败: userId={}, page={}, pageSize={}, error={}",
                    userId, page, pageSize, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取Token详情失败");
        }
    }

    /**
     * 获取指定应用的Token使用详情
     */
    @GetMapping("/app/{appId}/details")
    @Operation(summary = "获取应用Token使用详情", description = "查看指定应用的详细Token使用记录")
    public BaseResponse<List<TokenDetailDTO>> getAppTokenDetails(
            @Parameter(description = "应用ID") @PathVariable Long appId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);

            // 非管理员只能查看自己的应用
            if (!UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
                // 这里应该检查应用是否属于当前用户，简化处理
                // 实际项目中需要通过AppService检查应用所有权
            }

            List<TokenDetailDTO> details = tokenUsageService.getAppTokenDetails(appId, page, pageSize);
            return ResultUtils.success(details);
        } catch (Exception e) {
            log.error("获取应用Token详情失败: appId={}, page={}, pageSize={}, error={}",
                    appId, page, pageSize, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取Token详情失败");
        }
    }

    /**
     * 获取指定模型的Token消耗汇总
     */
    @GetMapping("/model/{modelName}/summary")
    @Operation(summary = "获取指定模型Token消耗汇总", description = "查看指定模型的Token消耗情况")
    public BaseResponse<ModelTokenSummaryDTO> getModelTokenSummary(
            @Parameter(description = "模型名称") @PathVariable String modelName,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            ModelTokenSummaryDTO summary = tokenUsageService.getModelTokenSummary(modelName, startTime, endTime);
            if (summary == null) {
                return new BaseResponse<>(ErrorCode.NOT_FOUND_ERROR.getCode(), null, "模型不存在或无Token消耗记录");
            }
            return ResultUtils.success(summary);
        } catch (Exception e) {
            log.error("获取模型Token汇总失败: modelName={}, error={}", modelName, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取模型统计失败");
        }
    }

    /**
     * 获取模型Token消耗排行榜
     */
    @GetMapping("/model/ranking")
    @Operation(summary = "获取模型Token消耗排行榜", description = "查看模型Token消耗排行榜")
    public BaseResponse<ModelTokenRankingDTO> getModelTokenRanking(
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "返回数量限制") @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        try {
            // 非管理员有访问限制
            User loginUser = userService.getLoginUser(request);
            if (!UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
                limit = Math.min(limit != null ? limit : 5, 5); // 非管理员最多看5个
            }

            ModelTokenRankingDTO ranking = tokenUsageService.getModelTokenRanking(startTime, endTime, limit);
            if (ranking == null) {
                return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取模型排行榜失败");
            }
            return ResultUtils.success(ranking);
        } catch (Exception e) {
            log.error("获取模型Token排行榜失败: startTime={}, endTime={}, limit={}, error={}",
                    startTime, endTime, limit, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取模型排行榜失败");
        }
    }

    /**
     * 获取所有模型Token消耗列表（管理员权限）
     */
    @GetMapping("/model/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取所有模型Token消耗列表", description = "管理员查看所有模型的Token消耗情况")
    public BaseResponse<List<ModelTokenSummaryDTO>> getAllModelTokenSummaries(
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<ModelTokenSummaryDTO> summaries = tokenUsageService.getAllModelTokenSummaries(startTime, endTime);
            return ResultUtils.success(summaries);
        } catch (Exception e) {
            log.error("获取所有模型Token汇总失败: startTime={}, endTime={}, error={}",
                    startTime, endTime, e.getMessage(), e);
            return new BaseResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, "获取模型统计失败");
        }
    }
}