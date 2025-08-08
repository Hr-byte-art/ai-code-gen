package com.wjh.aicodegen.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.dto.app.AppQueryRequest;
import com.wjh.aicodegen.model.entity.App;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.vo.app.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

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
}
