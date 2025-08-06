package com.wjh.aicodegen.scheduled;

import com.mybatisflex.core.query.QueryWrapper;
import com.wjh.aicodegen.model.entity.User;
import com.wjh.aicodegen.service.UserService;
import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @Author 王哈哈
 * @Date 2025/8/6 10:55:22
 * @Description 定时任务 - 检测VIP到期时间
 */
@Component
public class CheckVipExpirationDate {

    @Resource
    private UserService userService;

    /**
     * 定时任务 - 检测VIP到期时间（每天0点执行）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkVipExpirationDate() {
        // Vip 过期时间小于当前时间
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(User::getVipExpireTime).lt(LocalDateTime.now());
        List<User> expiredUserList = userService.list(queryWrapper);
        expiredUserList.forEach(user -> {
                    user.setVipNumber(null);
                    user.setUserRole("user");
                    userService.updateById(user);
                }
        );
    }
}
