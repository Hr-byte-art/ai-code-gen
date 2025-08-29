package com.wjh.aicodegen.utils;

import com.mybatisflex.core.update.UpdateChain;
import com.wjh.aicodegen.mapper.UserMapper;
import com.wjh.aicodegen.model.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author 王哈哈
 * @Date 2025/8/28 16:41:31
 * @Description 积分修改工具类
 */
@Component
@Slf4j
public class ModifyPointsUtils {
    @Resource
    private UserMapper userMapper;


    /**
     * 安全地增加用户积分，防止并发问题
     * @param userId 用户ID
     * @param points 增加积分数
     * @return 是否增加成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean safeAddPoints(Long userId, Integer points) {
        if (userId == null || points == null || points <= 0) {
            log.error("积分增加参数错误：userId={}, points={}", userId, points);
            return false;
        }

        // 增加重试机制，最多重试3次
        for (int retry = 0; retry < 3; retry++) {
            try {
                // 获取当前用户信息
                User currentUser = userMapper.selectOneById(userId);
                if (currentUser == null) {
                    log.error("用户不存在：userId={}", userId);
                    return false;
                }

                Integer currentPoints = currentUser.getIntegral();

                // 使用乐观锁增加积分
                boolean updateSuccess = UpdateChain.of(User.class)
                        .set(User::getIntegral, currentPoints + points)
                        .where(User::getId).eq(userId)
                        .and(User::getIntegral).eq(currentPoints) // 乐观锁条件
                        .update();

                if (updateSuccess) {
                    log.info("用户 {} 积分增加成功: +{} 分，当前积分: {} 分", userId, points, currentPoints + points);
                    return true;
                } else {
                    log.warn("积分增加失败（第{}次重试），可能存在并发冲突：用户ID={}, 增加积分={}", retry + 1, userId, points);

                    // 如果是最后一次重试，直接返回失败
                    if (retry == 2) {
                        log.error("积分增加最终失败：用户ID={}, 增加积分={}", userId, points);
                        return false;
                    }

                    // 指数退避策略：递增等待时间
                    Thread.sleep(100 + retry * 50);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("积分增加被中断：用户ID={}, 增加积分={}", userId, points);
                return false;
            } catch (Exception e) {
                log.error("积分增加异常：用户ID={}, 增加积分={}, 错误={}", userId, points, e.getMessage(), e);
                return false;
            }
        }

        return false;
    }


    /**
     * 安全地扣减用户积分，防止并发问题
     * @param userId 用户ID
     * @param points 扣减积分数
     * @return 是否扣减成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean safeDeductPoints(Long userId, Integer points) {
        if (userId == null || points == null || points <= 0) {
            log.error("积分扣减参数错误：userId={}, points={}", userId, points);
            return false;
        }

        // 预检查：先检查用户积分是否充足，避免无意义的重试
        User preCheckUser = userMapper.selectOneById(userId);
        if (preCheckUser == null) {
            log.error("用户不存在：userId={}", userId);
            return false;
        }
        if (preCheckUser.getIntegral() < points) {
            log.warn("用户积分不足（预检查）：userId={}, 当前积分={}, 需要扣减={}", userId, preCheckUser.getIntegral(), points);
            return false;
        }

        // 增加重试机制，最多重试3次
        for (int retry = 0; retry < 3; retry++) {
            try {
                // 获取当前用户信息
                User currentUser = userMapper.selectOneById(userId);
                if (currentUser == null) {
                    log.error("用户不存在：userId={}", userId);
                    return false;
                }

                Integer currentPoints = currentUser.getIntegral();
                // 重新校验积分是否足够（关键改进点）
                if (currentPoints < points) {
                    log.warn("用户积分不足：userId={}, 当前积分={}, 需要扣减={}", userId, currentPoints, points);
                    return false;
                }

                // 使用乐观锁扣减积分
                boolean updateSuccess = UpdateChain.of(User.class)
                        .set(User::getIntegral, currentPoints - points)
                        .where(User::getId).eq(userId)
                        .and(User::getIntegral).eq(currentPoints)// 乐观锁条件
                        .update();

                if (updateSuccess) {
                    log.info("用户 {} 积分扣减成功: -{} 分，剩余积分: {} 分", userId, points, currentPoints - points);
                    return true;
                } else {
                    log.warn("积分扣减失败（第{}次重试），可能存在并发冲突：用户ID={}, 扣减积分={}", retry + 1, userId, points);

                    // 如果是最后一次重试，直接返回失败
                    if (retry == 2) {
                        log.error("积分扣减最终失败：用户ID={}, 扣减积分={}", userId, points);
                        return false;
                    }

                    // 指数退避策略：递增等待时间
                    Thread.sleep(100 + retry * 50);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("积分扣减被中断：用户ID={}, 扣减积分={}", userId, points);
                return false;
            } catch (Exception e) {
                log.error("积分扣减异常：用户ID={}, 扣减积分={}, 错误={}", userId, points, e.getMessage(), e);
                return false;
            }
        }

        return false;
    }

}
