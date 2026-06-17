package com.ai.codeplatform.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@Import(BaseSecurityConfig.class)
@EnableWebSecurity
public class SecurityConfig {
    // 直接使用 common 模块的基础配置
}
