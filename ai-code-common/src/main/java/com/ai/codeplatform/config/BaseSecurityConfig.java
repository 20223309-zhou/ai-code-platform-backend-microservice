package com.ai.codeplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public class BaseSecurityConfig {

    /**
     * 基础的 Security 配置（供各个服务使用）
     */
    @Bean
    public SecurityFilterChain baseSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())
                // 允许 iframe 加载（编辑模式需要）
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                // 允许所有请求（具体的权限控制由各个服务自己配置）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // 禁用 HTTP Basic
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
