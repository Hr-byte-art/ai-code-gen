package com.wjh.aicodegen.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 木子宸
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                // 最小空闲连接数
                .setConnectionMinimumIdleSize(1)
                // 连接池大小
                .setConnectionPoolSize(10)
                // 空闲连接超时时间
                .setIdleConnectionTimeout(30000)
                // 连接超时时间
                .setConnectTimeout(5000)
                // 命令执行超时时间
                .setTimeout(3000)
                // 重试次数
                .setRetryAttempts(3)
                // 重试间隔时间
                .setRetryInterval(1500);
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
