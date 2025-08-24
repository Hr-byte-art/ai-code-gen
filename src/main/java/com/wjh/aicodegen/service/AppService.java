package com.wjh.aicodegen.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.common.BaseResponse;
import com.wjh.aicodegen.model.dto.app.AppAddRequest;
import com.wjh.aicodegen.model.dto.app.AppQueryRequest;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.vo.app.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 应用 服务层
 *
 * @author 王哈哈
 * @since 2025-08-07 17:32:51
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用视图对象
     *
     * @param app  app
     * @return appVO
     */
    AppVO getAppVO(App app);

    /**
     * 获取查询条件对象
     *
     * @param appQueryRequest appQueryRequest
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用视图对象列表
     *
     * @param appList appList
     * @return appVOList
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 聊天生成代码
     *
     * @param appId appQueryRequest
     * @param message  message
     * @param loginUser loginUser
     * @return string
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    
    /**
     * 部署应用
     *
     * @param appId appId
     * @param loginUser loginUser
     * @return string
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 应用下载
     *
     * @param appId appId
     * @param request request
     * @param response response
     */
    void downloadAppCode(Long appId, HttpServletRequest request, HttpServletResponse response);

    /**
     * 创建应用
     *
     * @param appAddRequest appAddRequest
     * @param loginUser loginUser
     * @param initPrompt initPrompt
     * @return appId
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser, String initPrompt);

    /**
     * 上传图片 （返回可访问的url）
     *
     * @param file file
     * @return string
     */
    String uploadPictures(MultipartFile file);

    /**
     * 获取应用构建状态
     *
     * @param appId appId
     * @return map
     */
    Map<String, Object> getBuildStatus(Long appId , HttpServletRequest request);
}
