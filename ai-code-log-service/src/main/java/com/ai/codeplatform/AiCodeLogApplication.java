package com.ai.codeplatform;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.ai.codeplatform.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ai.codeplatform.innerservice")
public class AiCodeLogApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCodeLogApplication.class, args);
    }
}
