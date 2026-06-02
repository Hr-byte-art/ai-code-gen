package com.wjh.aicodegen.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.ai.factory.AiCodeGenTypeRoutingServiceFactory;
import com.wjh.aicodegen.ai.factory.AiGenerateAppNameServiceFactory;
import com.wjh.aicodegen.ai.service.AiCodeGenTypeRoutingService;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.constant.UserConstant;
import com.wjh.aicodegen.convert.AppConverter;
import com.wjh.aicodegen.core.AiCodeGeneratorFacade;
import com.wjh.aicodegen.core.builder.VueProjectBuilder;
import com.wjh.aicodegen.core.handler.StreamHandlerExecutor;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.BuildEventSinkManager;
import com.wjh.aicodegen.manager.CosManager;
import com.wjh.aicodegen.manager.GenerationStreamManager;

import com.wjh.aicodegen.manager.TaskCancellationManager;
import com.wjh.aicodegen.mapper.AppMapper;
import com.wjh.aicodegen.model.dto.app.AppAddRequest;
import com.wjh.aicodegen.model.dto.app.AppQueryRequest;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.entity.CodeSkill;
import com.wjh.aicodegen.model.entity.CodeTemplate;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.enums.ChatHistoryMessageTypeEnum;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.model.enums.UserRoleEnum;
import com.wjh.aicodegen.model.vo.app.AppVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import com.wjh.aicodegen.model.enums.AiCallPurposeEnum;
import com.wjh.aicodegen.monitor.GlobalContextStorage;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.service.*;
import com.wjh.aicodegen.utils.ModifyPointsUtils;
import com.wjh.aicodegen.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author 王哈哈
 * @since 2025-08-07 17:32:51
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    // 部署所需
    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Value("${code.deploy-port:80}")
    private Integer deployPort;

    @Value("${code.deploy-path:code_deploy}")
    private String deployPath;

    /**
     * 路由决策缓存：prompt hash -> CodeGenType
     */
    private final ConcurrentHashMap<String, CodeGenTypeEnum> routeCache = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private AppConverter appConverter;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppSchemaRecordService appSchemaRecordService;

    @Resource
    private CodeSkillService codeSkillService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private TaskCancellationManager taskCancellationManager;

    @Resource
    private BuildEventSinkManager buildEventSinkManager;

    @Resource
    private GenerationStreamManager generationStreamManager;

    @Resource
    private AiGenerateAppNameServiceFactory AiGenerateAppNameServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    protected ProjectDownloadService projectDownloadService;

    // @Resource
    // private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;
    @Resource
    private CosManager cosManager;

    @Resource
    private com.wjh.aicodegen.core.deploy.NodeProcessManager nodeProcessManager;

    @Resource
    private ModifyPointsUtils modifyPointsUtils;

    @Resource
    private UserQuotaService userQuotaService;

    @Resource
    private CodeTemplateService codeTemplateService;
    @Resource
    private DesignTemplateService designTemplateService;

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = appConverter.toAppVO(app);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 如果同一应用已有生成任务，只订阅现有运行流，避免重复扣积分和重复写入用户消息
        if (generationStreamManager.hasActiveSession(appId)) {
            log.info("应用 {} 已存在运行中的生成流，本次请求直接订阅", appId);
            return generationStreamManager.subscribe(appId);
        }
        // 5. 获取应用的代码生成技能（带缓存）
        String codeGenTypeStr = app.getCodeGenType();
        CodeSkill skill = codeSkillService.getByCodeGenTypeCached(codeGenTypeStr);
        if (skill == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenTypeStr);
        }
        Integer requiredPoints = skill.getPointCost();
        // 工具增强模式（Vue/全栈）需要VIP或ADMIN权限
        if (!"none".equals(skill.getBuildStrategy())) {
            if (!loginUser.getUserRole().equals(UserRoleEnum.VIP.getValue()) &&
                    !loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())) {
                AppServiceImpl proxy = applicationContext.getBean(AppServiceImpl.class);
                proxy.updateAppCoverInNewTransaction(appId,
                        "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/noPermission.png");
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
            }
        }
        // 检查用户配额（每日/每月生成次数和 Token 消耗）
        if (!userQuotaService.checkQuota(loginUser.getId(), loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已达到今日或本月使用上限，请升级 VIP 或等待额度刷新");
        }
        // 预扣减积分，避免校验与扣减之间的时间差导致的并发问题
        boolean preDeductSuccess = modifyPointsUtils.safeDeductPoints(loginUser.getId(), requiredPoints);
        if (!preDeductSuccess) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("积分不足或扣减失败，需要%d积分", requiredPoints));
        }

        // 5. 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(),
                loginUser.getId());
        // 6. 设置监控上下文，标记为聊天交互阶段
        MonitorContext monitorContext = MonitorContext.builder()
                .userId(loginUser.getId().toString())
                .appId(appId.toString())
                // 用户与AI聊天交互
                .aiCallPurpose(AiCallPurposeEnum.CHAT_INTERACTION.getCode())
                .build();
        MonitorContextHolder.setContext(monitorContext);

        // 存储到全局上下文，供工具调用时使用
        GlobalContextStorage.storeContext(monitorContext);
        // 7. 取消已存在的任务（如果有）
        boolean existingTaskCancelled = taskCancellationManager.cancelTask(appId);
        if (existingTaskCancelled) {
            log.info("应用 {} 存在正在运行的任务，已取消旧任务", appId);
        }

        // 8. 设置初始封面（开始生成时设置为进行中状态的封面）
        if (app.getCover() == null || app.getCover().isEmpty()) {
            String defaultCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/defaultAppCover.jpg";
            app.setCover(defaultCover);
            updateById(app);
            log.info("为应用 {} 设置默认封面", appId);
        }
        // 9. 调用 AI 生成代码（流式）- 由后端后台订阅，不再绑定页面 SSE 连接生命周期
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, skill, appId)
                .doOnCancel(() -> {
                    try {
                        String cancelCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/userCancel.png";
                        app.setCover(cancelCover);
                        updateById(app);

                        boolean penaltyDeductSuccess = modifyPointsUtils.safeDeductPoints(loginUser.getId(), 3);
                        if (penaltyDeductSuccess) {
                            log.warn("REACTOR收到取消信号：应用 {} AI生成流已被中断，已更新为取消状态，扣减用户{}惩罚积分3分", appId, loginUser.getId());
                        } else {
                            log.error("应用 {} 取消时扣减惩罚积分失败，用户ID: {}", appId, loginUser.getId());
                        }
                    } catch (Exception e) {
                        log.error("处理任务取消时发生异常：应用ID {}, 用户ID {}, 错误: {}", appId, loginUser.getId(), e.getMessage(), e);
                    }
                })
                .doOnError(error -> {
                    try {
                        String failedCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/createFailed.png";
                        app.setCover(failedCover);
                        updateById(app);

                        boolean refundSuccess = modifyPointsUtils.safeAddPoints(loginUser.getId(), requiredPoints);
                        if (refundSuccess) {
                            log.info("应用 {} AI生成失败，已返还用户 {} 积分 {} 分", appId, loginUser.getId(), requiredPoints);
                        } else {
                            log.error("应用 {} AI生成失败，但返还积分失败，用户ID: {}, 应返还积分: {}", appId, loginUser.getId(), requiredPoints);
                        }

                        log.error("应用 {} AI生成失败，已更新为失败状态: {}", appId, error.getMessage());
                    } catch (Exception e) {
                        log.error("处理AI生成失败时发生异常：应用ID {}, 用户ID {}, 错误: {}", appId, loginUser.getId(), e.getMessage(), e);
                    }
                })
                .contextWrite(context -> MonitorContextHolder.putContextToReactor(context, monitorContext));

        // 10. 收集 AI 响应内容并在完成后记录到对话历史。后台订阅独立于页面连接。
        generationStreamManager.start(appId);
        Disposable generationTask = streamHandlerExecutor
                .doExecute(codeStream, chatHistoryService, appId, loginUser, skill.getBuildStrategy())
                .doOnNext(chunk -> generationStreamManager.emit(appId, chunk))
                .doOnComplete(() -> {
                    try {
                        log.info("用户 {} 代码生成完成，已预扣减{}个积分", loginUser.getId(), requiredPoints);
                        String successCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/defaultAppCover.jpg";
                        app.setCover(successCover);
                        updateById(app);
                        log.info("应用 {} AI代码生成完成，已更新为成功状态", appId);
                    } catch (Exception e) {
                        log.error("处理任务完成时发生异常：应用ID {}, 用户ID {}, 错误: {}", appId, loginUser.getId(), e.getMessage(), e);
                    }
                    generationStreamManager.complete(appId);
                })
                .doOnError(error -> generationStreamManager.error(appId, "AI回复失败: " + error.getMessage()))
                .doFinally(signalType -> {
                    taskCancellationManager.unregisterTask(appId);
                    MonitorContextHolder.clearContext();
                    GlobalContextStorage.removeContext(appId.toString());
                    log.debug("🧹 应用 {} 任务资源和上下文已完整清理，结束信号: {}", appId, signalType);
                })
                .subscribe();
        taskCancellationManager.registerTask(appId, generationTask);
        log.info("应用 {} 后台AI代码生成任务已启动", appId);

        return generationStreamManager.subscribe(appId);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAppCoverInNewTransaction(Long appId, String coverUrl) {
        App app = this.getById(appId);
        if (app != null) {
            app.setCover(coverUrl);
            this.updateById(app);
        }
    }

    @Override
    public boolean cancelGenerationTask(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 验证用户是否有权限取消该应用的任务，仅本人可以取消
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限取消该应用的任务");
        }

        // 4. 调用任务管理器取消任务
        log.warn("开始取消应用 {} 的任务，调用 taskCancellationManager.cancelTask()", appId);
        boolean cancelled = taskCancellationManager.cancelTask(appId);
        log.warn("取消任务结果：应用 {} cancelled={}", appId, cancelled);

        // 强制中断：如果找到活跃任务，立即中断当前线程的AI处理
        if (cancelled) {
            log.warn("应用 {} 任务已取消", appId);
        }

        // 5. 设置取消封面（无论是否有正在运行的任务，用户主动取消都设置取消封面）
        String cancelCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/userCancel.png";
        app.setCover(cancelCover);
        updateById(app);

        // 6. 记录取消操作
        if (cancelled) {
            log.info("用户主动取消：应用 {} 代码生成已停止", appId);
            // 可以选择在对话历史中记录取消事件
            try {
                chatHistoryService.addChatMessage(appId, "任务已被用户取消",
                        ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                generationStreamManager.error(appId, "任务已被用户取消");
            } catch (Exception e) {
                // 记录失败不影响取消操作
                log.warn("记录任务取消到对话历史失败: {}", e.getMessage());
            }
        } else {
            generationStreamManager.error(appId, "任务已被用户取消");
            log.info("用户请求取消：应用 {} 无活跃任务，已设置取消状态", appId);
        }

        return cancelled;
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        CodeSkill skill = codeSkillService.getByCodeGenTypeCached(codeGenType);
        String buildStrategy = skill != null ? skill.getBuildStrategy() : "none";
        if ("fullstack".equals(buildStrategy)) {
            sourceDir = resolveFullstackProjectDir(appId, sourceDir);
            sourceDirPath = sourceDir.getAbsolutePath();
            if (StrUtil.isNotBlank(app.getDeployKey()) && app.getDeployedTime() != null && nodeProcessManager.isReady(appId)) {
                String expressUrl = nodeProcessManager.getUrl(appId);
                log.info("全栈应用已部署且服务可用，直接复用现有地址: appId={}, url={}", appId, expressUrl);
                return expressUrl;
            }
        }
        // 6. 检查源目录是否存在
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 项目构建
        if ("vue".equals(buildStrategy)) {
            boolean result = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "构建项目失败,请重新尝试构建");
            File distDir = new File(sourceDir, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue项目构建成功，但是未成功创建 dist 目录");
            sourceDir = distDir;
        } else if ("fullstack".equals(buildStrategy)) {
            // 全栈项目：构建前端 + 复制到 server/public + 启动 Express
            File frontendDir = new File(sourceDir, "frontend");
            File serverDir = new File(sourceDir, "server");

            if (frontendDir.exists() && serverDir.exists()) {
                // 重新构建前端
                log.info("重新构建全栈项目前端...");
                boolean buildSuccess = vueProjectBuilder.buildProject(frontendDir.getAbsolutePath());
                ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "全栈项目前端构建失败，请检查构建日志");

                // 复制 dist 到 server/public
                File frontendDistDir = new File(frontendDir, "dist");
                File publicDir = new File(serverDir, "public");
                ThrowUtils.throwIf(!frontendDistDir.exists(), ErrorCode.SYSTEM_ERROR, "全栈项目前端构建失败，未生成 dist 目录");
                FileUtil.copyContent(frontendDistDir, publicDir, true);
                log.info("前端 dist 已复制到 server/public");
            }

            // 启动 Express 服务
            String serverPath = serverDir.getAbsolutePath();
            int port = nodeProcessManager.startServer(appId, serverPath);
            if (port <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Express 服务启动失败");
            }

            // 更新应用部署信息
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployKey(deployKey);
            updateApp.setDeployedTime(LocalDateTime.now());
            this.updateById(updateApp);

            // 返回 Express 服务 URL
            String expressUrl = nodeProcessManager.getUrl(appId);
            log.info("全栈项目部署成功: appId={}, url={}", appId, expressUrl);
            generateAppScreenshotAsync(appId, expressUrl);
            return expressUrl;
        }
        // 8. 复制文件到部署目录（先清空再复制，避免旧文件残留）
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        File deployDir = new File(deployDirPath);
        try {
            if (deployDir.exists()) {
                FileUtil.del(deployDir);
            }
            FileUtil.copyContent(sourceDir, deployDir, true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8.1 如果没有 index.html，尝试将唯一的 HTML 文件重命名为 index.html
        if (!new File(deployDir, "index.html").exists()) {
            File[] htmlFiles = deployDir.listFiles((dir, name) -> name.endsWith(".html"));
            if (htmlFiles != null && htmlFiles.length == 1) {
                File indexFile = new File(deployDir, "index.html");
                htmlFiles[0].renameTo(indexFile);
                log.info("将 {} 重命名为 index.html", htmlFiles[0].getName());
            }
        }
        // 8.2 修复 index.html 中的资源路径（绝对路径 → 相对路径）
        File indexHtml = new File(deployDir, "index.html");
        if (indexHtml.exists()) {
            try {
                String content = FileUtil.readUtf8String(indexHtml);
                // 将 src="/assets/ 和 href="/assets/ 替换为相对路径
                content = content.replace("src=\"/assets/", "src=\"./assets/")
                                 .replace("href=\"/assets/", "href=\"./assets/")
                                 .replace("src=\"/", "src=\"./")
                                 .replace("href=\"/", "href=\"./");
                FileUtil.writeUtf8String(content, indexHtml);
                log.info("已修复 index.html 中的资源路径");
            } catch (Exception e) {
                log.warn("修复 index.html 路径失败: {}", e.getMessage());
            }
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s:%s/%s/%s/index.html", deployHost, deployPort, deployPath, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    @Override
    public String getDeployedAppUrl(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        ThrowUtils.throwIf(StrUtil.isBlank(app.getDeployKey()) || app.getDeployedTime() == null,
                ErrorCode.NOT_FOUND_ERROR, "应用尚未部署");

        CodeSkill skill = codeSkillService.getByCodeGenTypeCached(app.getCodeGenType());
        String buildStrategy = skill != null ? skill.getBuildStrategy() : "none";
        if ("fullstack".equals(buildStrategy)) {
            if (nodeProcessManager.isReady(appId)) {
                String expressUrl = nodeProcessManager.getUrl(appId);
                ThrowUtils.throwIf(StrUtil.isBlank(expressUrl), ErrorCode.SYSTEM_ERROR, "全栈服务地址不存在");
                return expressUrl;
            }
            return null;
        }

        return String.format("%s:%s/%s/%s/index.html", deployHost, deployPort, deployPath, app.getDeployKey());
    }

    private File resolveFullstackProjectDir(Long appId, File defaultDir) {
        if (defaultDir.exists() && new File(defaultDir, "frontend").exists() && new File(defaultDir, "server").exists()) {
            return defaultDir;
        }
        String[] possibleDirNames = {
                "fullstack_" + appId,
                "vue_project_" + appId,
                "react_ts_" + appId,
                "nextjs_" + appId
        };
        for (String dirName : possibleDirNames) {
            File dir = new File(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
            if (dir.exists() && new File(dir, "frontend").exists() && new File(dir, "server").exists()) {
                return dir;
            }
        }
        return defaultDir;
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 清理全栈应用创建的数据库表
        try {
            appSchemaRecordService.dropAndRemoveByAppId(appId);
        } catch (Exception e) {
            log.error("清理应用数据库表失败: {}", e.getMessage());
        }
        // 停止全栈应用的 Express 进程
        try {
            nodeProcessManager.stopServer(appId);
        } catch (Exception e) {
            log.error("停止 Express 进程失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            try {
                String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
                if (StrUtil.isBlank(screenshotUrl)) {
                    log.warn("应用 {} 截图生成失败，保留当前封面", appId);
                    return;
                }
                App updateApp = this.getById(appId);
                if (updateApp == null) {
                    log.warn("应用 {} 不存在，跳过封面截图更新", appId);
                    return;
                }
                updateApp.setCover(screenshotUrl);
                boolean updated = this.updateById(updateApp);
                ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
            } catch (Throwable e) {
                log.warn("应用 {} 异步截图失败，不影响部署主流程: {}", appId, e.getMessage(), e);
            }
        });
    }

    @Override
    public void downloadAppCode(Long appId, HttpServletRequest request, HttpServletResponse response) {
        // 1. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 2. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 3. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 4. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 5. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 6. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser, String initPrompt) {
        // 如果提供了模板标识，将模板内容注入到 prompt 中
        String templateKey = appAddRequest.getTemplateKey();
        if (StrUtil.isNotBlank(templateKey)) {
            CodeTemplate template = codeTemplateService.getByKey(templateKey);
            if (template != null) {
                initPrompt = "请基于以下模板进行定制化修改，保留模板的核心结构和设计，根据用户需求进行调整。\n\n"
                        + "【模板参考】\n" + template.getTemplateContent() + "\n\n"
                        + "【用户需求】\n" + initPrompt;
                codeTemplateService.incrementUseCount(template.getId());
                log.info("使用模板 {} 生成应用，模板ID: {}", templateKey, template.getId());
            }
        }

        // 如果提供了设计风格标识，将 DESIGN.md 设计规范注入到 prompt 中
        String designKey = appAddRequest.getDesignKey();
        if (StrUtil.isNotBlank(designKey)) {
            String designContent = designTemplateService.getDesignTemplate(designKey);
            if (designContent != null) {
                // 转义 DESIGN.md 中的 {{...}} 模板变量，防止 LangChain4j 误解析
                String escapedContent = designContent.replace("{{", "").replace("}}", "");
                initPrompt = initPrompt + "\n\n## 设计风格要求\n\n"
                        + "请严格按照以下 DESIGN.md 的设计规范生成代码：\n\n"
                        + escapedContent;
                log.info("使用设计风格 {} 生成应用", designKey);
            }
        }

        // 构造入库对象
        App app = appConverter.toApp(appAddRequest);
        app.setUserId(loginUser.getId());

        // AiGenerateAppNameService aiGenerateAppNameService =
        // AiGenerateAppNameServiceFactory.createAiGenerateAppNameService();
        // app.setAppName(aiGenerateAppNameService.generateAppName(initPrompt));

        String appName = StrUtil.sub(initPrompt, 0, Math.min(initPrompt.length(), 12));
        app.setAppName(appName);
        app.setIsDelete(0);

        // 先插入数据库获取appId
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 设置监控上下文，标记为路由阶段
        MonitorContext monitorContext = MonitorContext.builder()
                .userId(loginUser.getId().toString())
                .appId(app.getId().toString())
                // 使用aiCallPurpose字段存储AI调用用途
                .aiCallPurpose(AiCallPurposeEnum.ROUTING.getCode())
                .build();
        MonitorContextHolder.setContext(monitorContext);

        // 存储到全局上下文，供工具调用时使用
        GlobalContextStorage.storeContext(monitorContext);

        // 确定代码生成类型
        String requestedCodeGenType = appAddRequest.getCodeGenType();
        if (StrUtil.isNotBlank(requestedCodeGenType) && !"auto".equals(requestedCodeGenType)) {
            // 用户指定了具体类型，直接使用
            app.setCodeGenType(requestedCodeGenType);
            log.info("用户指定代码生成类型: {}", requestedCodeGenType);
        } else {
            // 自动模式：调用 AI 路由决策
            String promptHash = hashPrompt(initPrompt);
            CodeGenTypeEnum codeGenTypeEnum = routeCache.get(promptHash);
            if (codeGenTypeEnum != null) {
                log.info("命中路由决策缓存: promptHash={}, type={}", promptHash, codeGenTypeEnum.getValue());
            } else {
                AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory
                        .createAiCodeGenTypeRoutingService();
                codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
                routeCache.put(promptHash, codeGenTypeEnum);
                log.info("路由决策已缓存: promptHash={}, type={}", promptHash, codeGenTypeEnum.getValue());
            }
            app.setCodeGenType(codeGenTypeEnum.getValue());
        }

        // 更新数据库中的codeGenType
        this.updateById(app);

        // 路由完成后更新上下文，标记为代码生成阶段
        monitorContext.setAiCallPurpose(AiCallPurposeEnum.CODE_GENERATION.getCode());
        MonitorContextHolder.setContext(monitorContext);

        // 更新全局上下文
        GlobalContextStorage.storeContext(monitorContext);
        return app.getId();
    }

    @Override
    public String uploadPictures(MultipartFile file) {
        // 1. 获取文件名
        String fileName = file.getOriginalFilename();

        // 2. 验证文件名安全性，防止目录遍历攻击
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不合法");
        }

        // 3. 获取文件后缀
        String suffix = FileUtil.getSuffix(fileName);

        // 4. 生成唯一文件名避免覆盖
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;

        // 5. 文件路径
        String filePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "ChatPictures" + File.separator
                + uniqueFileName;
        Path path = Paths.get(filePath);
        // 6. 确保目录存在
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建目录失败：" + e.getMessage());
        }
        // 7. 写入文件
        try {
            FileUtil.writeBytes(file.getBytes(), filePath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件写入失败：" + e.getMessage());
        }
        // 8. 文件上传
        File uploadFile = new File(filePath);
        String result;
        try {
            String uploadFilePath = StrUtil.format("{}/{}", "ChatPicture",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
            result = cosManager.uploadFile(uploadFilePath, uploadFile);
        } finally {
            // 9. 清理临时文件，确保即使上传失败也能删除
            FileUtil.del(uploadFile);
        }
        return result;
    }

    @Override
    public Map<String, Object> getBuildStatus(Long appId, HttpServletRequest request) {
        // 参数校验和权限检查
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查询构建状态");
        }
        // 检查构建状态
        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
        File projectDir = new File(projectPath);
        File distDir = new File(projectDir, "dist");
        Map<String, Object> buildStatus = new HashMap<>();
        buildStatus.put("appId", appId);
        buildStatus.put("projectExists", projectDir.exists());
        buildStatus.put("distExists", distDir.exists());
        // 同步构建模式下总是false
        buildStatus.put("isBuilding", false);
        if (distDir.exists()) {
            buildStatus.put("status", "completed");
            buildStatus.put("message", "构建已完成");
            buildStatus.put("buildTime", distDir.lastModified());
        } else if (projectDir.exists()) {
            buildStatus.put("status", "pending");
            buildStatus.put("message", "项目已生成，等待构建");
        } else {
            buildStatus.put("status", "not_found");
            buildStatus.put("message", "项目不存在");
        }
        return buildStatus;
    }

    @Override
    public Flux<String> getBuildEventStream(Long appId) {
        return buildEventSinkManager.getOrCreateSink(appId);
    }

    @Override
    public boolean hasActiveGenerationStream(Long appId, User loginUser) {
        validateGenerationStreamAccess(appId, loginUser);
        return generationStreamManager.hasActiveSession(appId);
    }

    @Override
    public Flux<String> getGenerationEventStream(Long appId, User loginUser) {
        validateGenerationStreamAccess(appId, loginUser);
        return generationStreamManager.subscribe(appId);
    }

    private void validateGenerationStreamAccess(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
    }

    /**
     * 对 prompt 做 SHA-256 hash，用作缓存 key
     */
    private String hashPrompt(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(prompt.hashCode());
        }
    }
}
