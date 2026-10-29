package com.zhiguang.service;

import com.zhiguang.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AI 每日配额（按用户，Redis 原子计数）。
 * <p>
 * 计入所有会真实消耗 LLM 的端点（润色/流式润色/写作助手/一键生成/总结 force 等）。
 * 键按天分区：ai:quota:{userId}:{yyyyMMdd}，TTL 25h 余量，次日自然过期。
 * Agent 设计文档（§4.7）复用本服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private static final String KEY_PREFIX = "ai:quota:";
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redisTemplate;
    private final AiConfig aiConfig;

    /**
     * 尝试消耗一次配额。
     *
     * @return false 表示今日已超限，调用方应返回 429
     */
    public boolean tryConsume(Long userId) {
        if (userId == null || aiConfig.getDailyQuota() <= 0) {
            return true;
        }
        String key = key(userId);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofHours(25));
            }
            boolean allowed = count == null || count <= aiConfig.getDailyQuota();
            if (!allowed) {
                log.info("AI 每日配额已用完: userId={}", userId);
            }
            return allowed;
        } catch (Exception e) {
            // 配额组件故障不应阻断业务（成本防护还有端点级限流兜底）
            log.warn("AI 配额检查失败，放行本次调用: userId={}", userId, e);
            return true;
        }
    }

    /** 今日剩余次数（配额关闭时返回 -1 表示不限） */
    public long getRemaining(Long userId) {
        if (userId == null || aiConfig.getDailyQuota() <= 0) {
            return -1;
        }
        try {
            String value = redisTemplate.opsForValue().get(key(userId));
            long used = value == null ? 0 : Long.parseLong(value);
            return Math.max(0, aiConfig.getDailyQuota() - used);
        } catch (Exception e) {
            return -1;
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now().format(DAY);
    }
}
