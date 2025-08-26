package com.wjh.aicodegen.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.ai.factory.AiCodeGenTypeRoutingServiceFactory;
import com.wjh.aicodegen.ai.factory.AiGenerateAppNameServiceFactory;
import com.wjh.aicodegen.ai.service.AiCodeGenTypeRoutingService;
import com.wjh.aicodegen.ai.service.AiGenerateAppNameService;
import com.wjh.aicodegen.constant.AppConstant;
import com.wjh.aicodegen.constant.UserConstant;
import com.wjh.aicodegen.convert.AppConverter;
import com.wjh.aicodegen.core.AiCodeGeneratorFacade;
import com.wjh.aicodegen.core.builder.VueProjectBuilder;
import com.wjh.aicodegen.core.handler.StreamHandlerExecutor;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.CosManager;
import com.wjh.aicodegen.manager.TaskCancellationManager;
import com.wjh.aicodegen.mapper.AppMapper;
import com.wjh.aicodegen.model.dto.app.AppAddRequest;
import com.wjh.aicodegen.model.dto.app.AppQueryRequest;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.enums.ChatHistoryMessageTypeEnum;
import com.wjh.aicodegen.model.enums.CodeGenTypeEnum;
import com.wjh.aicodegen.model.vo.app.AppVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import com.wjh.aicodegen.monitor.MonitorContext;
import com.wjh.aicodegen.monitor.MonitorContextHolder;
import com.wjh.aicodegen.service.*;
import com.wjh.aicodegen.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    @Value("${cos.client.host}")
    private String cosClientHost;

    @Resource
    private UserService userService;

    @Resource
    private AppConverter appConverter;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private TaskCancellationManager taskCancellationManager;

    @Resource
    private AiGenerateAppNameServiceFactory AiGenerateAppNameServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    protected ProjectDownloadService projectDownloadService;

//    @Resource
//    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;
    @Resource
    private CosManager cosManager;


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
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 设置监控上下文
        MonitorContext monitorContext = MonitorContext.builder()
                .userId(loginUser.getId().toString())
                .appId(appId.toString())
                .build();
        MonitorContextHolder.setContext(monitorContext);
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
        
        // 9. 调用 AI 生成代码（流式）- 在这里就开始注册任务管理
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId)
                // 在AI流的最开始就注册任务管理
                .doOnSubscribe(subscription -> {
                    // 注册任务到管理器 - 这是真正的AI生成流开始的地方
                    taskCancellationManager.registerTask(appId, subscription);
                    log.info("应用 {} 开始AI代码生成，任务已注册", appId);
                })
                .doOnCancel(() -> {
                    // AI任务取消时的处理 - 设置取消封面
                    String cancelCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/userCancel.png";
                    app.setCover(cancelCover);
                    updateById(app);
                    log.warn("✅ REACTOR收到取消信号：应用 {} AI生成流已被中断，已更新为取消状态", appId);
                })
                .doOnError(error -> {
                    // AI任务出错时的处理 - 设置失败封面
                    String failedCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/createFailed.png";
                    app.setCover(failedCover);
                    updateById(app);
                    log.error("应用 {} AI生成失败，已更新为失败状态: {}", appId, error.getMessage());
                })
                // 将监控上下文传递到Reactor流中
                .contextWrite(context -> MonitorContextHolder.putContextToReactor(context, monitorContext));
                
        // 10. 收集 AI 响应内容并在完成后记录到对话历史
        Flux<String> managedStream = streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                .doOnComplete(() -> {
                    // 任务完成时的处理 - 设置成功封面
                    String successCover = "https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/defaultAppCover.jpg";
                    app.setCover(successCover);
                    updateById(app);
                    log.info("应用 {} AI代码生成完成，已更新为成功状态", appId);
                })
                .doFinally(signalType -> {
                    // 流结束时清理（无论成功/失败/取消）
                    taskCancellationManager.unregisterTask(appId);
                    MonitorContextHolder.clearContext();
                    log.debug("应用 {} 任务资源已清理，结束信号: {}", appId, signalType);
                });
                
        return managedStream;

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
            } catch (Exception e) {
                // 记录失败不影响取消操作
                log.warn("记录任务取消到对话历史失败: {}", e.getMessage());
            }
        } else {
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
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue 项目的特殊处理 ：执行构建操作
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // 构建项目
            boolean result = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "构建项目失败,请重新尝试构建");
            // 检查 Dist 目录是否存在
            File distDir = new File(sourceDir, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue项目构建成功，但是未成功创建 dist 目录");
            // 构建完成后，将构建项目的 dist 目录复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
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
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }


    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = this.getById(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
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
        // 构造入库对象
        App app = appConverter.toApp(appAddRequest);
        app.setUserId(loginUser.getId());

//        AiGenerateAppNameService aiGenerateAppNameService = AiGenerateAppNameServiceFactory.createAiGenerateAppNameService();
//        app.setAppName(aiGenerateAppNameService.generateAppName(initPrompt));

        String appName = StrUtil.sub(initPrompt, 0, Math.min(initPrompt.length(), 12));
        app.setAppName(appName);


        // 调用 Ai 决策使用类型 （多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenTypeEnum.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
        String filePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "ChatPictures" + File.separator + uniqueFileName;
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
            String uploadFilePath = StrUtil.format("{}/{}", "ChatPicture", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
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
}
