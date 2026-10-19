package com.ai.codeplatform.ratelimit.aspect;

import com.ai.codeplatform.ai.model.entity.User;
import com.ai.codeplatform.exception.BusinessException;
import com.ai.codeplatform.exception.ErrorCode;
import com.ai.codeplatform.ratelimit.annotation.RateLimit;
import com.ai.codeplatform.utils.IpUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

import static com.ai.codeplatform.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 限流切面（user-service 版）。
 * <p>
 * 注解与 Redisson 配置在 common 模块共享；切面因为要读本服务的登录态，各服务自备一份。
 * 与 app-service 版本的差异：登录用户直接从 Session 取，不经过 InnerUserService（那会多一次 RPC）。
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Resource
    private RedissonClient redissonClient;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        String key = generateRateLimitKey(point, rateLimit);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        try {
            rateLimiter.expire(Duration.ofHours(1));
            rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), RateIntervalUnit.SECONDS);
        } catch (Exception e) {
            // fail-open：限流组件故障不能把注册/登录拖垮，只有真正命中限流才拦截
            log.error("限流器初始化失败，放行本次请求, key: {}", key, e);
            return;
        }
        try {
            if (!rateLimiter.tryAcquire(1)) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流判断失败，放行本次请求, key: {}", key, e);
        }
    }

    private String generateRateLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        switch (rateLimit.limitType()) {
            case API:
                MethodSignature signature = (MethodSignature) point.getSignature();
                Method method = signature.getMethod();
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".").append(method.getName());
                break;
            case USER:
                Object userObj = request == null ? null : request.getSession().getAttribute(USER_LOGIN_STATE);
                if (userObj instanceof User loginUser && loginUser.getId() != null) {
                    keyBuilder.append("user:").append(loginUser.getId());
                } else {
                    // 未登录用户退化为 IP 限流
                    keyBuilder.append("ip:").append(IpUtils.getClientIp(request));
                }
                break;
            case IP:
                keyBuilder.append("ip:").append(IpUtils.getClientIp(request));
                break;
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return keyBuilder.toString();
    }
}
