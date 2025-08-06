package com.wjh.aicodegen.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.convert.UserConverter;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.mapper.UserMapper;
import com.wjh.aicodegen.model.dto.user.UserQueryRequest;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.entity.VipCode;
import com.wjh.aicodegen.model.enums.UserRoleEnum;
import com.wjh.aicodegen.model.vo.user.LoginUserVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.service.VipCodeService;
import com.wjh.aicodegen.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.wjh.aicodegen.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author 王哈哈
 * @since 2025-08-05 16:50:59
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserConverter userConverter;

    @Resource
    private VipCodeService vipCodeService;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String shareCode) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        // 用户名为 用户+随机6位
        user.setUserName("用户" + genRandomChars());
        user.setUserRole(UserRoleEnum.USER.getValue());
        // 使用当前时间搓 加 随机数 作为 shareCode
        user.setShareCode(String.valueOf(System.currentTimeMillis() + genRandomChars()));
        // 如果邀请码不为空，则查询邀请码
        if (StrUtil.isNotBlank(shareCode)) {
            // 查询邀请码
            User one = this.getOne(QueryWrapper.create().eq(User::getShareCode, shareCode));
            if (one != null) {
                // 添加邀请人
                user.setInviteUser(one.getId());
            }
        }

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 生成随机字符
     *
     * @return 随机字符
     */
    private String genRandomChars() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        // 生成 6 - 10的随机数
        int numLen = (int) (Math.random() * 5) + 6;

        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numLen; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 获取加密密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "wang-haha";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }


    /**
     * 获取脱敏用户信息
     *
     * @param user  用户
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        return userConverter.toLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        return userConverter.toUserVO(user);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getUserPage(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public UserVO vipCodeRedemption(String vipCode, HttpServletRequest request) {
        // 获取当前用户
        User currentUser = this.getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper queryWrapper = new QueryWrapper().create()
                .eq(VipCode::getVipCode, vipCode);
        VipCode vipCodeInfo = vipCodeService.getOne(queryWrapper);
        if (vipCodeInfo == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验兑换码是否过期
        ThrowUtils.throwIf(vipCodeInfo.getExpDate().isBefore(LocalDateTime.now())  , ErrorCode.OPERATION_ERROR , "兑换码已过期");
        //检查是否查过最大使用人数
        ThrowUtils.throwIf(vipCodeInfo.getMaxUseNum() <= vipCodeInfo.getUseNum() , ErrorCode.OPERATION_ERROR , "兑换码已超最大使用人数");
        // 获取当前兑换的时间
        Integer effectiveDay = vipCodeInfo.getEffectiveDay();
        // 校验是否使用过该验证码
        String currentUserVipCode = currentUser.getVipCode();
        if (currentUserVipCode !=  null){
            ThrowUtils.throwIf(currentUserVipCode.equals(vipCodeInfo.getVipCode()) , ErrorCode.OPERATION_ERROR , "请勿重复使用");
        }
        currentUser.setVipCode(vipCode);
        // 修改用户角色 -- 如果是admin 就不变
        if (!currentUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())){
            currentUser.setUserRole(UserRoleEnum.VIP.getValue());
        }
        currentUser.setVipCode(vipCodeInfo.getVipCode());
        currentUser.setVipExpireTime(LocalDateTime.now().plusDays(effectiveDay));
        // 时间戳
        long currentTimeMillis = System.currentTimeMillis();
        // 设置会员编号(时间戳+过期时间)
        currentUser.setVipNumber(currentTimeMillis + effectiveDay);
        // 更新用户
        this.updateById(currentUser);
        return this.getUserVO(currentUser);
    }

    @Override
    public List<UserVO> myInvited(HttpServletRequest request) {
        // 获取当前用户
        User loginUser = getLoginUser(request);
        QueryWrapper queryWrapper = new QueryWrapper().eq(User::getInviteUser, loginUser.getId());
        List<User> list = this.list(queryWrapper);
        return this.getUserVOList(list);
    }


}
