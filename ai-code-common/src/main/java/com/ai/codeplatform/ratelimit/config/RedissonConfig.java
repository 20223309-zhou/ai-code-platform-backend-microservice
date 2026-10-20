package com.ai.codeplatform.ratelimit.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置（限流用）。
 * <p>
 * 放在 common，app-service 与 user-service 共用同一份连接参数。
 * <p>
 * 🔴 必须带 {@link ConditionalOnProperty}：本类位于 {@code com.ai.codeplatform.ratelimit.config}，
 * 所有以 {@code com.ai.codeplatform} 为根扫描的启动类（如 ai-code-screenshot）都会扫到它。
 * 而 screenshot-service 既不依赖 Redis、也没有 {@code spring.data.redis.*} 配置，
 * 无条件注册会让 {@code ${spring.data.redis.host}} 解析失败，
 * 直接抛 PlaceholderResolutionException 导致整个服务启动失败。
 * 用「有 spring.data.redis.host 才装配」把不需要限流的服务排除在外。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
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
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10)
                .setIdleConnectionTimeout(30000)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
