package com.ai.codeplatform.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(BaseSecurityConfig.class)
public class SecurityConfig {
    // 直接使用 common 模块的基础配置
}
