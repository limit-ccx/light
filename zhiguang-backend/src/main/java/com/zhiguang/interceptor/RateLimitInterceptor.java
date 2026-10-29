package com.zhiguang.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.RateLimitProperties;
import com.zhiguang.dto.ApiResponse;
import com.zhiguang.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * 全局限流拦截器
 * <p>
 * 使用 Redis Lua 原子计数，并基于服务端认证结果或远端 IP 生成客户端标识。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "return count",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate,
                                RateLimitProperties rateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String endpoint = request.getRequestURI();
        // 限流桶按命中的规则模式分键（而非原始 URI），路径变量不再稀释限流、也不再撑爆 key 空间
        String rulePattern = rateLimitProperties.resolveRulePattern(request.getMethod(), endpoint);
        String bucketEndpoint = rulePattern != null ? rulePattern : endpoint;
        String clientId = getClientId(request);
        int[] config = rateLimitProperties.getEndpointConfig(request.getMethod(), endpoint);
        int limit = config[0];
        int window = config[1];
        String key = String.format("rate_limit:%s:%s", bucketEndpoint, clientId);

        try {
            Long count = redisTemplate.execute(
                    INCREMENT_WITH_EXPIRE,
                    Collections.singletonList(key),
                    String.valueOf(window)
            );

            if (count != null && count > limit) {
                log.warn("限流触发: endpoint={}, clientId={}, count={}, limit={}",
                        endpoint, clientId, count, limit);

                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                ApiResponse apiResponse = ApiResponse.error(429,
                        String.format("请求过于频繁，请在 %d 秒后重试", window));
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return false;
            }

            log.debug("限流检查通过: endpoint={}, clientId={}, count={}, limit={}",
                    endpoint, clientId, count, limit);
        } catch (Exception e) {
            log.error("限流检查失败，降级为放行: endpoint={}, clientId={}", endpoint, clientId, e);
        }

        return true;
    }

    private String getClientId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user && user.getId() != null) {
            return "user:" + user.getId();
        }
        // forward-headers-strategy=framework 下 getRemoteAddr() 反映 XFF 首段（客户端可伪造），
        // 改用 IpUtil（取 XFF 末跳），避免轮换 XFF 头绕过登录爆破限流
        return "ip:" + com.zhiguang.util.IpUtil.getClientIp(request);
    }
}
