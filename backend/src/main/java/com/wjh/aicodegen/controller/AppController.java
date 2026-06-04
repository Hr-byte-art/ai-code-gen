package com.wjh.aicodegen.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.common.DeleteRequest;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.constant.UserConstant;
import com.wjh.aicodegen.convert.AppConverter;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.model.dto.app.*;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.ai.factory.AiCodeGenTypeRoutingServiceFactory;
import com.wjh.aicodegen.ai.service.AiCodeGenTypeRoutingService;
import com.wjh.aicodegen.model.vo.app.AppVO;
import com.wjh.aicodegen.reteLimit.annotation.RateLimit;
import com.wjh.aicodegen.reteLimit.enums.RateLimitType;
import com.wjh.aicodegen.service.AppService;
import com.wjh.aicodegen.service.CodeSkillService;
import com.wjh.aicodegen.service.ProjectDownloadService;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.utils.CacheUtils;
import com.wjh.aicodegen.utils.ResultUtils;
import com.wjh.aicodegen.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author 王哈哈
 * @since 2025-08-07 17:32:51
 */
@RestController
@RequestMapping("/app")
@Slf4j
@Tag(name = "应用接口", description = "应用的相关接口")
public class AppController {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;


    @Resource
    private ProjectDownloadService projectDownloadService;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private AppConverter appConverter;

    @Resource
    private CodeSkillService codeSkillService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    private ServerSentEvent<String> toGenerationSse(String chunk) {
        Map<String, String> wrapper = Map.of("d", chunk);
        return ServerSentEvent.<String>builder()
                .data(JSONUtil.toJsonStr(wrapper))
                .build();
    }


    /**
     * 获取路由推荐
     * AI 预判用户需求应该使用哪种生成模式
     */
    @PostMapping("/routing/recommend")
    @Operation(summary = "获取路由推荐", description = "AI 预判用户需求应该使用哪种生成模式")
    public BaseResponse<RoutingRecommendation> getRoutingRecommendation(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(appAddRequest == null || StrUtil.isBlank(appAddRequest.getInitPrompt()), ErrorCode.PARAMS_ERROR);

        String initPrompt = appAddRequest.getInitPrompt();
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum recommended = routingService.routeCodeGenType(initPrompt);

        CodeSkill skill = codeSkillService.getByCodeGenType(recommended.getValue());
        if (skill == null) {
            skill = codeSkillService.getByCodeGenType("html");
        }

        boolean isFullstack = "fullstack".equals(skill.getBuildStrategy());

        // 构建推荐结果
        RoutingRecommendation rec = RoutingRecommendation.builder()
                .recommendedType(recommended.getValue())
                .recommendedName(skill.getName())
                .reason(isFullstack ? "您的需求涉及数据存储、用户系统或后端 API，建议使用全栈模式" : "您的需求主要是前端展示，建议使用前端模式")
                .fullstack(isFullstack)
                .pointCost(skill.getPointCost())
                .build();

        // 如果推荐全栈，提供纯前端备选方案
        if (isFullstack) {
            CodeSkill frontendSkill = codeSkillService.getByCodeGenType("vue_project");
            if (frontendSkill != null) {
                rec.setAlternativeType("vue_project");
                rec.setAlternativeName(frontendSkill.getName());
                rec.setAlternativePointCost(frontendSkill.getPointCost());
            }
        }

        return ResultUtils.success(rec);
    }


    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    @PostMapping("/create")
    @Operation(summary = "创建应用")
    public BaseResponse<Long> createApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        Long appId = appService.createApp(appAddRequest, loginUser, initPrompt);
        return ResultUtils.success(appId);
    }


    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求
     * @return 更新结果
     */
    @PostMapping("/update")
    @Operation(summary = "更新应用（用户只能更新自己的应用名称）")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long appId = appUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(appId);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(appId);
        app.setAppName(appUpdateRequest.getAppName());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "删除应用（用户只能删除自己的应用）")
    @CacheEvict(value = "good_App_List_Cache", allEntries = true)
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     *
     * @param id      应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据 id 获取应用详情")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取当前用户创建的应用列表")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @Operation(summary = "分页获取精选应用列表")
    @Cacheable(
            value = "good_App_List_Cache",
            key = "T(com.wjh.aicodegen.utils.CacheKeyUtils).generateKey(#appQueryRequest)",
            condition = "#appQueryRequest.pageNum <= 10"
    )
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest)
                .and("deployedTime IS NOT NULL")
                .and("deployKey IS NOT NULL AND deployKey <> ''");
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @Operation(summary = "删除应用（管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(value = "good_App_List_Cache", allEntries = true)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @Operation(summary = "更新应用（管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(value = "good_App_List_Cache", allEntries = true)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (AppConstant.GOOD_APP_PRIORITY.equals(appAdminUpdateRequest.getPriority())
                && (oldApp.getDeployedTime() == null || StrUtil.isBlank(oldApp.getDeployKey()))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未上线或不可预览的应用不能设为首页公开样本");
        }
        App app = appConverter.toApp(appAdminUpdateRequest);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @Operation(summary = "分页获取应用列表(管理员)")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }


    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @Operation(summary = "管理员根据 id 获取应用详情")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 管理员清除应用相关缓存
     *
     * @return 清除结果
     */
    @PostMapping("/admin/cache/clear")
    @Operation(summary = "清除应用相关缓存（管理员）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> clearAppCache() {
        try {
            cacheUtils.clearGoodAppListCache();
            return ResultUtils.success(true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用缓存清除失败：" + e.getMessage());
        }
    }

    /**
     * 测试时间序列化格式（仅用于调试）
     *
     * @return 测试应用数据
     */
    @GetMapping("/test/time")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "测试时间序列化格式")
    public BaseResponse<AppVO> testTimeFormat() {
        App app = new App();
        app.setId(1L);
        app.setAppName("测试应用");
        app.setCreateTime(LocalDateTime.now());
        app.setDeployedTime(LocalDateTime.now().minusDays(1));
        
        AppVO appVO = appConverter.toAppVO(app);
        return ResultUtils.success(appVO);
    }

    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    @Operation(summary = "应用聊天生成代码（流式 SSE）")
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER, rate = 5, rateInterval = 60, message = "AI 对话请求过于频繁，请稍后再试")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 转换为 ServerSentEvent 格式
        return Flux.defer(() -> {
                    // 调用服务生成代码（流式）
                    Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
                    return contentFlux.map(this::toGenerationSse);
                })
                .doOnError(error -> {
                    // SSE流错误处理
                    log.warn("SSE流发生错误，应用ID: {}, 错误: {}", appId, error.getMessage());
                })
                .onErrorResume(error -> {
                    // 如果是连接断开错误，优雅结束流
                    if (error instanceof java.io.IOException &&
                            error.getMessage() != null &&
                            error.getMessage().contains("已建立的连接")) {
                        log.info(" SSE连接已断开，应用ID: {}, 优雅结束流", appId);
                        return Flux.empty();
                    }
                    String errorMessage = error instanceof BusinessException
                            ? error.getMessage()
                            : "生成过程中出现异常，请稍后重试";
                    if (!(error instanceof BusinessException)) {
                        log.error("SSE 流处理异常，应用ID: {}", appId, error);
                    }
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("bizError")
                            .data(JSONUtil.toJsonStr(Map.of("message", errorMessage)))
                            .build());
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 查询应用是否存在运行中的代码生成流
     */
    @Operation(summary = "查询应用是否存在运行中的代码生成流")
    @GetMapping("/chat/gen/active")
    public BaseResponse<Boolean> hasActiveGenerationStream(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(appService.hasActiveGenerationStream(appId, loginUser));
    }

    /**
     * 订阅运行中的代码生成流
     */
    @Operation(summary = "订阅运行中的代码生成流")
    @GetMapping(value = "/chat/gen/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> subscribeGenerationStream(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        return appService.getGenerationEventStream(appId, loginUser)
                .map(this::toGenerationSse)
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build()));
    }

    /**
     * 取消应用代码生成任务
     *
     * @param appId   应用 ID
     * @param request 请求对象
     * @return 取消结果
     */
    @Operation(summary = "取消应用代码生成任务")
    @PostMapping("/cancel/{appId}")
    public BaseResponse<Boolean> cancelGenerationTask(@PathVariable Long appId, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务取消任务
        boolean result = appService.cancelGenerationTask(appId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    @Operation(summary = "应用部署")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    /**
     * 查询应用已部署访问地址，不触发构建或启动
     *
     * @param appId   应用 ID
     * @param request 请求对象
     * @return 访问地址
     */
    @GetMapping("/deploy/url")
    @Operation(summary = "查询应用已部署访问地址")
    public BaseResponse<String> getDeployedAppUrl(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        User loginUser = getLoginUserIfPresent(request);
        String deployUrl = appService.getDeployedAppUrl(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    private User getLoginUserIfPresent(HttpServletRequest request) {
        try {
            return userService.getLoginUser(request);
        } catch (BusinessException e) {
            if (ErrorCode.NOT_LOGIN_ERROR.getCode() == e.getCode()) {
                return null;
            }
            throw e;
        }
    }

    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    @Operation(summary = "应用下载")
    public void downloadAppCode(@PathVariable Long appId, HttpServletRequest request, HttpServletResponse response) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        appService.downloadAppCode(appId, request, response);
    }


    @PostMapping("/upload/Pictures")
    @Operation(summary = "上传图片")
    public BaseResponse<String> uploadPictures(@RequestParam("file") MultipartFile file) {
        String upload = appService.uploadPictures(file);
        return ResultUtils.success(upload);
    }


    /**
     * 获取应用构建状态(轮询查询)
     *
     * <p>
     *     轮询查询应用构建状态，直到构建完成或者失败, 返回构建结果
     *     <span style="color:red;"><br>注意：该接口仅供前端轮询调用，请勿直接访问<br></span>
     *     <span style="color:red;">
     *         当前使用的逻辑实际是没有使用轮询的方法，是在后端直接使用的同步构建 VUE 项目，
     *         这个方法在用户的访问量大的时候不适用，但是当前还可以接受。
     *         如果想换成轮询实现的话，需要将 JsonMessageStreamHandler 类的<br>
     *         // 异步构建 Vue 项目<br>
     *         // String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project" + appId;<br>
     *         // vueProjectBuilder.buildProjectAsync(projectPath);<br>
     *         内容放开，然后将 AiCodeGeneratorFacade 类中的 <br>
     *         // 异步构建 Vue 项目<br>
     *         String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project" + appId;<br>
     *         vueProjectBuilder.buildProjectAsync(projectPath);<br>
     *         注释<br>
     *     </span>
     * </p>
     *
     * @param appId 应用ID
     * @return 构建状态
     */


    @Operation(summary = "获取应用构建状态(轮询查询)")
    @GetMapping("/build/status/{appId}")
    public BaseResponse<Map<String, Object>> getBuildStatus(@PathVariable Long appId, HttpServletRequest request) {
        Map<String, Object> buildStatus = appService.getBuildStatus(appId, request);
        return ResultUtils.success(buildStatus);
    }

    @Operation(summary = "构建事件SSE流", description = "订阅应用构建状态的实时事件")
    @GetMapping(value = "/build/events/{appId}", produces = "text/event-stream")
    public Flux<String> getBuildEvents(@PathVariable Long appId) {
        return appService.getBuildEventStream(appId);
    }

}
