package com.ai.codeplatform.exception;

import com.ai.codeplatform.annotation.InnerApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 内部接口专用异常处理。
 * <p>
 * 🔴 必须与 {@link GlobalExceptionHandler} 区分开：
 * 全局那个按前端约定返回 <b>HTTP 200</b> + {@code BaseResponse}，
 * 而服务间调用走 Feign，只有<b>非 2xx</b> 才会触发调用方的 {@code ErrorDecoder}。
 * 若内部接口也返回 200，Feign 会把错误体当成返回值去反序列化（{@code DecodeException}），
 * 业务码 {@code 40100/50001} 全部丢失。
 * <p>
 * {@code @Order(HIGHEST_PRECEDENCE)} 不能省：两个 advice 都能匹配同一个控制器时，
 * 优先级高的先接；全局那个默认是 {@code LOWEST_PRECEDENCE}。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(annotations = InnerApi.class)
public class InnerApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        log.warn("内部接口业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(statusOf(e.getCode()))
                .body(Map.of("code", e.getCode(),
                        "message", e.getMessage() == null ? "内部服务调用失败" : e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        log.error("内部接口异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", ErrorCode.SYSTEM_ERROR.getCode(),
                        "message", "内部服务调用失败"));
    }

    /**
     * 按业务码分段映射 HTTP 状态：4xxxx 客户端/业务错误，5xxxx 服务端错误。
     * Feign 侧只看「非 2xx」，具体语义由 body 里的 code 决定。
     */
    private HttpStatus statusOf(int code) {
        return (code >= 40000 && code < 50000) ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
