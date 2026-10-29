package com.zhiguang.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JWT 黑名单服务
 * <p>
 * 用于在用户登出、被封禁、密码修改后使旧 Token 立即失效。
 * 将 jti（Token ID）写入 Redis，过期时间与 Token 剩余有效期一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 将 Token 加入黑名单
     *
     * @param tokenJti     Token 的唯一标识（使用 userId+issuedAt 作为 jti）
     * @param expirationMs Token 剩余有效期（毫秒）
     */
    public void blacklist(String tokenJti, long expirationMs) {
        try {
            String key = BLACKLIST_PREFIX + tokenJti;
            redisTemplate.opsForValue().set(key, "1", expirationMs, TimeUnit.MILLISECONDS);
            log.info("Token 已加入黑名单: jti={}", tokenJti);
        } catch (Exception e) {
            log.error("Token 加入黑名单失败: jti={}", tokenJti, e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String tokenJti) {
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + tokenJti);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查 Token 黑名单失败: jti={}", tokenJti, e);
            // Redis 异常时降级为不在黑名单（不影响业务可用性）
            return false;
        }
    }
}
