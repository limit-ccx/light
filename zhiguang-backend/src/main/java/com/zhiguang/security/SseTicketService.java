package com.zhiguang.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * SSE 短时 Ticket 服务
 * <p>
 * 用于替代在 URL query 参数中直接传递 JWT。
 * 前端先调用 /api/auth/sse-ticket 获取一次性短时 ticket（30 秒有效），
 * 然后 EventSource 使用 ?ticket=xxx 连接 SSE，后端用 ticket 换取用户身份。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseTicketService {

    private static final String TICKET_PREFIX = "sse:ticket:";
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TICKET_LENGTH = 32;
    private static final long TICKET_TTL_SECONDS = 30;

    private final StringRedisTemplate redisTemplate;

    /**
     * 生成一次性 SSE ticket，绑定用户 ID
     */
    public String generateTicket(Long userId) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TICKET_LENGTH);
        for (int i = 0; i < TICKET_LENGTH; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        String ticket = sb.toString();

        try {
            redisTemplate.opsForValue().set(
                    TICKET_PREFIX + ticket,
                    userId.toString(),
                    TICKET_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            log.debug("生成 SSE ticket: userId={}", userId);
        } catch (Exception e) {
            log.error("生成 SSE ticket 失败: userId={}", userId, e);
            throw new IllegalStateException("SSE ticket 服务暂不可用", e);
        }

        return ticket;
    }

    /**
     * 消费 ticket（一次性），返回用户 ID
     * 消费后立即从 Redis 删除
     */
    public Long consumeTicket(String ticket) {
        if (ticket == null || ticket.isEmpty()) return null;

        try {
            String key = TICKET_PREFIX + ticket;
            String userIdStr = redisTemplate.opsForValue().getAndDelete(key);
            if (userIdStr != null) {
                return Long.parseLong(userIdStr);
            }
        } catch (Exception e) {
            log.error("消费 SSE ticket 失败", e);
        }
        return null;
    }
}
