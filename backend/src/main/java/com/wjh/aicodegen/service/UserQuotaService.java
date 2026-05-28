package com.wjh.aicodegen.service;

import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.entity.UserQuota;
import com.wjh.aicodegen.model.vo.UserQuotaVO;

/**
 * 用户配额配置 服务层。
 *
 * @author 王哈哈
 */
public interface UserQuotaService extends IService<UserQuota> {

    /**
     * 根据角色获取配额配置
     *
     * @param role 角色
     * @return 配额配置
     */
    UserQuota getQuotaByRole(String role);

    /**
     * 检查用户是否还有配额（生成次数和 Token 消耗）
     *
     * @param userId 用户ID
     * @param role   用户角色
     * @return true=有配额, false=超额
     */
    boolean checkQuota(Long userId, String role);

    /**
     * 获取当前用户的配额使用情况
     *
     * @param userId 用户ID
     * @param role   用户角色
     * @return 配额 VO
     */
    UserQuotaVO getUserQuotaVO(Long userId, String role);
}
