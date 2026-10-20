package com.ai.codeplatform.innerservice.feign;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 全局配置。
 * 注册自定义 {@link ErrorDecoder} 覆盖 Feign 默认实现
 * （{@code FeignClientsConfiguration#errorDecoder()} 带 {@code @ConditionalOnMissingBean}，
 * 所以这里声明的 Bean 会生效）。
 * 放在 client 模块，所有依赖 client 的服务都会通过 {@code com.ai.codeplatform} 的组件扫描拿到它。
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new BusinessErrorDecoder();
    }
}
