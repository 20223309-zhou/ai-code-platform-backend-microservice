package com.ai.codeplatform.innerservice.feign;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ai.codeplatform.exception.BusinessException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 把服务端的错误响应还原成 {@link BusinessException}。
 * 服务端（内部接口）出错时返回非 2xx + {@code {"code":50001,"message":"..."}}，
 * 默认的 {@code ErrorDecoder} 只会抛 {@code FeignException}，业务码就丢了。
 * 这里解析 body 后重新抛 {@code BusinessException}，让它能像本地调用一样
 * 交给调用方自己的 {@code GlobalExceptionHandler} 处理（前端逻辑不用改）。
 * <p>
 * 解析不出来（例如 503 空响应、非 JSON）就委托默认实现，保留原有语义。
 */
@Slf4j
public class BusinessErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder delegate = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        Response.Body body = response.body();
        if (body != null) {
            try {
                // body 只能读一次，读失败就走默认实现
                String text = Util.toString(body.asReader(StandardCharsets.UTF_8)).trim();
                // 网关 503 之类经常回 HTML 错误页，先做个廉价判断，避免为预期内的分支打异常栈
                if (text.startsWith("{")) {
                    JSONObject json = JSONUtil.parseObj(text);
                    if (json.containsKey("code")) {
                        int code = json.getInt("code");
                        String message = json.getStr("message", "内部服务调用失败");
                        log.warn("内部服务返回业务异常: {} -> code={}, message={}", methodKey, code, message);
                        return new BusinessException(code, message);
                    }
                } else {
                    log.debug("内部服务错误响应不是 JSON，回退默认处理: {}", methodKey);
                }
            } catch (Exception e) {
                log.warn("解析内部服务错误响应失败，回退默认处理: {} - {}", methodKey, e.getMessage());
            }
        }
        return delegate.decode(methodKey, response);
    }
}
