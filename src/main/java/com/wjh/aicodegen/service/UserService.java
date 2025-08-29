package com.wjh.aicodegen.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wjh.aicodegen.model.dto.user.ChangePasswordRequest;
import com.wjh.aicodegen.model.dto.user.UserQueryRequest;
import com.wjh.aicodegen.model.dto.user.UserUpdateRequest;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.vo.user.LoginUserVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户 服务层。
 *
 * @author 王哈哈
 * @since 2025-08-05 16:50:59
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword , String shareCode);

    /**
     * 获取加密密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户信息列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getUserPage(UserQueryRequest userQueryRequest);

    /**
     * 会员码兑换会员
     *
     * @param request
     * @return
     */
    UserVO vipCodeRedemption(String vipCode, HttpServletRequest request);

    /**
     * 获取当前用户邀请的会员
     *
     * @return
     */
    List<UserVO> myInvited(HttpServletRequest request);

    /**
     * 修改密码
     *
     * @param changePasswordRequest 修改密码请求
     * @param loginUser       当前登录用户
     * @return 修改结果
     */
    Boolean changePassword(ChangePasswordRequest changePasswordRequest, User loginUser);

    /**
     * 上传用户头像
     *
     * @param file       头像文件
     * @param loginUser  当前登录用户
     * @return 头像URL
     */
    String uploadAvatar(MultipartFile file, User loginUser);

    /**
     * 部分更新用户信息
     *
     * @param userId            用户ID
     * @param userUpdateRequest 更新请求
     * @return 更新结果
     */
    boolean updateUserPartial(Long userId, UserUpdateRequest userUpdateRequest);

    /**
     * 签到
     *
     * @param request
     * @return
     */
    Integer signIn(HttpServletRequest request);
}
