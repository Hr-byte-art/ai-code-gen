package com.wjh.aicodegen.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ModifierUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wjh.aicodegen.convert.UserConverter;
import com.wjh.aicodegen.exception.BusinessException;
import com.wjh.aicodegen.exception.ErrorCode;
import com.wjh.aicodegen.manager.CosManager;

import com.wjh.aicodegen.mapper.UserMapper;
import com.wjh.aicodegen.model.dto.user.ChangePasswordRequest;
import com.wjh.aicodegen.model.dto.user.UserQueryRequest;
import com.wjh.aicodegen.model.dto.user.UserUpdateRequest;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.model.entity.VipCode;
import com.wjh.aicodegen.model.enums.UserRoleEnum;
import com.wjh.aicodegen.model.vo.user.LoginUserVO;
import com.wjh.aicodegen.model.vo.user.UserVO;
import com.wjh.aicodegen.service.UserService;
import com.wjh.aicodegen.service.VipCodeService;
import com.wjh.aicodegen.utils.ModifyPointsUtils;
import com.wjh.aicodegen.utils.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.wjh.aicodegen.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author 王哈哈
 * @since 2025-08-05 16:50:59
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserConverter userConverter;

    @Resource
    private VipCodeService vipCodeService;

    @Resource
    private CosManager cosManager;

    @Resource
    private ModifyPointsUtils modifyPointsUtils;

    final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword, String shareCode) {
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
        user.setUserAvatar("https://ai-code-gen-1340059484.cos.ap-chengdu.myqcloud.com/DefaultAvatar.jpg");
        // 生成简短易记的分享码
        user.setShareCode(generateShareCode());
        // 处理邀请码逻辑
        User inviter = null;
        if (StrUtil.isNotBlank(shareCode)) {
            // 查询邀请人
            inviter = this.getOne(QueryWrapper.create().eq(User::getShareCode, shareCode));
            if (inviter == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请码无效");
            }
            // 设置邀请关系
            user.setInviteUser(inviter.getId());
            log.info("用户注册使用邀请码: {}, 邀请人ID: {}", shareCode, inviter.getId());
        }
        // 设置默认积分 50
        user.setIntegral(50);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        
        // 用户注册成功后，为邀请人增加积分奖励
        if (inviter != null) {
            boolean addPointsSuccess = modifyPointsUtils.safeAddPoints(inviter.getId(), 50);
            if (addPointsSuccess) {
                log.info("邀请奖励发放成功：邀请人 {} 获得 50 积分，新用户 {} 注册成功", inviter.getId(), user.getId());
            } else {
                log.error("邀请奖励发放失败：邀请人 {} 未能获得积分，新用户 {} 已注册成功", inviter.getId(), user.getId());
                // 注意：这里不抛异常，避免影响用户注册成功，只记录错误日志
            }
        }
        
        return user.getId();
    }

    /**
     * 生成随机字符
     *
     * @return 随机字符
     */
    private String genRandomChars() {

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
     * 生成简短易记的分享码
     * 格式：AI + 6位字母数字混合
     * 示例：AI6K8M9X
     *
     * @return 8位分享码
     */
    private String generateShareCode() {
        Random random = new Random();
        // 最大尝试次数，防止无限循环
        int maxAttempts = 100;
        int attempts = 0;

        String shareCode;
        do {
            attempts++;
            if (attempts > maxAttempts) {
                // 如果尝试次数过多，说明可用组合不足，抛出异常
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "分享码生成失败，可用组合不足，请联系管理员");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("SQ");

            // 生成6位随机字符
            for (int i = 0; i < 6; i++) {
                int index = random.nextInt(CHARACTERS.length());
                sb.append(CHARACTERS.charAt(index));
            }

            shareCode = sb.toString();
        } while (isShareCodeExists(shareCode));

        return shareCode;
    }

    /**
     * 检查分享码是否已存在
     *
     * @param shareCode 分享码
     * @return 是否存在
     */
    private boolean isShareCodeExists(String shareCode) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("shareCode", shareCode);
        return this.count(queryWrapper) > 0;
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
    @Transactional(rollbackFor = Exception.class)
    public UserVO vipCodeRedemption(String vipCode, HttpServletRequest request) {
        ThrowUtils.throwIf(StrUtil.isBlank(vipCode), ErrorCode.PARAMS_ERROR, "会员码不能为空");
        User currentUser = this.getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 重试机制处理并发
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            QueryWrapper queryWrapper = new QueryWrapper()
                    .eq(VipCode::getVipCode, vipCode);
            VipCode vipCodeInfo = vipCodeService.getOne(queryWrapper);
            if (vipCodeInfo == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }

            // 校验兑换码是否过期
            ThrowUtils.throwIf(vipCodeInfo.getExpDate().isBefore(LocalDateTime.now()),
                    ErrorCode.OPERATION_ERROR, "兑换码已过期");

            // 检查是否超过最大使用人数
            Integer useNum = vipCodeInfo.getUseNum() == null ? 0 : vipCodeInfo.getUseNum();
            Integer maxUseNum = vipCodeInfo.getMaxUseNum() == null ? 0 : vipCodeInfo.getMaxUseNum();
            ThrowUtils.throwIf(maxUseNum <= useNum,
                    ErrorCode.OPERATION_ERROR, "兑换码已超最大使用人数");

            // 校验是否使用过该验证码
            String currentUserVipCode = currentUser.getVipCode();
            if (currentUserVipCode != null) {
                ThrowUtils.throwIf(currentUserVipCode.equals(vipCodeInfo.getVipCode()),
                        ErrorCode.OPERATION_ERROR, "请勿重复使用");
            }

            // 使用乐观锁更新兑换码使用次数
            boolean vipCodeUpdateSuccess = UpdateChain.of(VipCode.class)
                    .set(VipCode::getUseNum, useNum + 1)
                    .where(VipCode::getId).eq(vipCodeInfo.getId())
                    .and(VipCode::getUseNum).eq(useNum)
                    .update();

            if (vipCodeUpdateSuccess) {
                Integer effectiveDay = vipCodeInfo.getEffectiveDay();
                LocalDateTime nextVipExpireTime = LocalDateTime.now().plusDays(effectiveDay);
                String nextUserRole = UserRoleEnum.ADMIN.getValue().equals(currentUser.getUserRole())
                        ? UserRoleEnum.ADMIN.getValue()
                        : UserRoleEnum.VIP.getValue();

                boolean userUpdateSuccess = UpdateChain.of(User.class)
                        .set(User::getVipCode, vipCode)
                        .set(User::getUserRole, nextUserRole)
                        .set(User::getVipExpireTime, nextVipExpireTime)
                        .where(User::getId).eq(currentUser.getId())
                        .update();
                if (userUpdateSuccess) {
                    currentUser.setVipCode(vipCode);
                    currentUser.setUserRole(nextUserRole);
                    currentUser.setVipExpireTime(nextVipExpireTime);
                    log.info("用户 {} 成功兑换VIP码: {}", currentUser.getId(), vipCode);
                    return this.getUserVO(currentUser);
                } else {
                    // 用户更新失败，需要回滚兑换码使用次数
                    UpdateChain.of(VipCode.class)
                            .set(VipCode::getUseNum, useNum)
                            .where(VipCode::getId).eq(vipCodeInfo.getId())
                            .update();
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户信息更新失败");
                }
            }

            // 兑换码更新失败，等待后重试
            try {
                Thread.sleep(50 + i * 10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "兑换被中断");
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "兑换码使用失败，请重试");
    }

    @Override
    public List<UserVO> myInvited(HttpServletRequest request) {
        // 获取当前用户
        User loginUser = getLoginUser(request);
        QueryWrapper queryWrapper = new QueryWrapper().eq(User::getInviteUser, loginUser.getId());
        List<User> list = this.list(queryWrapper);
        return this.getUserVOList(list);
    }

    @Override
    public Boolean changePassword(ChangePasswordRequest changePasswordRequest, User loginUser) {
        String oldPassword = changePasswordRequest.getOldPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        String confirmPassword = changePasswordRequest.getConfirmPassword();

        if (StringUtils.isAnyBlank(oldPassword, newPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String encryptOldPassword = getEncryptPassword(oldPassword);
        String encryptNewPassword = getEncryptPassword(newPassword);

        String userPassword = loginUser.getUserPassword();
        boolean equals = userPassword.equals(encryptOldPassword);
        if (!equals) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "旧密码错误");
        }
        loginUser.setUserPassword(encryptNewPassword);
        return this.updateById(loginUser);
    }

    @Override
    public String uploadAvatar(MultipartFile file, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 校验
        String fileExtension = verify(file);
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + fileExtension;

        // 创建临时文件
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFilePath = tempDir + File.separator + uniqueFileName;
        File tempFile = new File(tempFilePath);

        try {
            // 保存文件到临时目录
            file.transferTo(tempFile);

            // 生成COS存储路径：avatar/用户ID/文件名
            String cosKey = String.format("/avatar/%d/%s", loginUser.getId(), uniqueFileName);

            // 上传到COS
            String avatarUrl = cosManager.uploadFile(cosKey, tempFile);

            if (avatarUrl == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
            }

            // 更新用户头像URL
            loginUser.setUserAvatar(avatarUrl);
            boolean updateResult = this.updateById(loginUser);

            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户头像失败");
            }

            return avatarUrl;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件处理失败：" + e.getMessage());
        } finally {
            // 11. 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    public boolean updateUserPartial(Long userId, UserUpdateRequest userUpdateRequest) {
        if (userId == null || userUpdateRequest == null) {
            return false;
        }

        // 先获取当前用户信息
        User currentUser = this.getById(userId);
        if (currentUser == null) {
            return false;
        }

        boolean hasUpdate = false;

        // 只有当字段不为空且不为空字符串时才更新
        if (StringUtils.isNotBlank(userUpdateRequest.getUserName())) {
            currentUser.setUserName(userUpdateRequest.getUserName());
            hasUpdate = true;
        }

        if (StringUtils.isNotBlank(userUpdateRequest.getUserAvatar())) {
            currentUser.setUserAvatar(userUpdateRequest.getUserAvatar());
            hasUpdate = true;
        }

        if (userUpdateRequest.getUserProfile() != null) {
            currentUser.setUserProfile(userUpdateRequest.getUserProfile());
            hasUpdate = true;
        }

        // 如果没有需要更新的字段，直接返回成功
        if (!hasUpdate) {
            return true;
        }

        return this.updateById(currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer signIn(HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 固定时间戳，避免在重试过程中时间不一致的问题
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime signInTime = LocalDateTime.now();

        // 预检查：先检查今天是否已签到，避免在重试循环中重复检查
        User preCheckUser = this.getById(loginUser.getId());
        if (preCheckUser.getRecentlySignedIn() != null &&
                preCheckUser.getRecentlySignedIn().isAfter(todayStart)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今天已经签到过了");
        }

        int maxRetryCount = 3;
        // 重试机制，最多重试3次
        for (int i = 0; i < maxRetryCount; i++) {
            // 重新获取最新的用户信息
            User currentUser = this.getById(loginUser.getId());

            // 再次检查今天是否已签到（防止并发情况下的重复签到）
            if (currentUser.getRecentlySignedIn() != null &&
                    currentUser.getRecentlySignedIn().isAfter(todayStart)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "今天已经签到过了");
            }

            // 处理积分可能为null的情况
            Integer currentIntegral = currentUser.getIntegral();
            int integralToUse = (currentIntegral != null) ? currentIntegral : 0;

            // 使用乐观锁更新积分和签到时间（使用固定时间戳）
            boolean updateSuccess = UpdateChain.of(User.class)
                    .set(User::getIntegral, integralToUse + 20)
                    // 使用固定时间戳
                    .set(User::getRecentlySignedIn, signInTime)
                    .where(User::getId).eq(loginUser.getId())
                    // 保持原值比较
                    .and(User::getIntegral).eq(currentIntegral)
                    .and(User::getRecentlySignedIn).eq(currentUser.getRecentlySignedIn())
                    .update();

            if (updateSuccess) {
                log.info("用户 {} 签到成功，获得20积分，当前积分: {}", loginUser.getId(), currentUser.getIntegral() + 20);
                return currentUser.getIntegral() + 20;
            }

            // 更新失败，可能存在并发，使用指数退避策略重试
            try {
                // 指数退避策略：递增等待时间
                Thread.sleep(100 + i * 50);
                log.warn("签到失败（第{}次重试），可能存在并发冲突：用户ID={}", i + 1, loginUser.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签到被中断");
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签到失败，请重试");
    }


    @NotNull
    private static String verify(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不合法");
        }

        // 3. 检查文件类型（只允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能上传图片文件");
        }

        // 4. 检查文件大小（限制为5MB）
        long fileSize = file.getSize();
        if (fileSize > 5 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过5MB");
        }

        // 5. 生成唯一文件名
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return fileExtension;
    }


}
