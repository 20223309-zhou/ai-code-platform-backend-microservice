package com.ai.codeplatform;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.ai.codeplatform.mapper")
@EnableCaching
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ai.codeplatform.innerservice")
public class AiCodeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCodeAppApplication.class, args);
    }
}
