package com.ai.codeplatform.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「服务间内部接口」的控制器。
 * <p>
 * 被标记的控制器走 {@code InnerApiExceptionHandler}：
 * 业务异常返回<b>非 2xx</b> + {@code {code, message}}，
 * 以便调用方的 Feign {@code ErrorDecoder} 能还原出 {@code BusinessException}。
 * <p>
 * 普通对外接口不使用本注解，仍由 {@code GlobalExceptionHandler} 统一返回
 * HTTP 200 + {@code BaseResponse}（前端依赖这种约定）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InnerApi {
}
