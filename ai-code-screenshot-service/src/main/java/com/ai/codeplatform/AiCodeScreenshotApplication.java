package com.ai.codeplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ai.codeplatform.innerservice")
public class AiCodeScreenshotApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCodeScreenshotApplication.class, args);
    }
}
