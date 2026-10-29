package com.zhiguang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Redis 缓存服务
 * <p>
 * 特性：
 * - Cache-Aside 旁路缓存模式
 * - 空值缓存防止缓存穿透
 * - 随机 TTL 防止缓存雪崩
 * - 浏览量增量计数器与脏集合
 */
@Slf4j
@Service
public class CacheService {

    private static final String NULL_PLACEHOLDER = "__NULL__";
    private static final String VIEW_COUNTER_PREFIX = "article:view:";
    private static final String VIEW_DIRTY_SET = "article:view:dirty";
    private static final Duration VIEW_COUNTER_TTL = Duration.ofHours(48);
    private static final int CACHE_SCAN_COUNT = 200;

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * 缓存重建单飞锁（PERF-P2-1）：热 key 失效瞬间，并发请求只放一个回源，
     * 其余在锁上等待并复用结果（双查缓存），避免击穿到 DB。
     * 实例内互斥；多实例部署时可升级为 Redisson 分布式锁。
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Object> rebuildLocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    public CacheService(StringRedisTemplate redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 单飞执行缓存重建：先双查缓存，未命中才互斥回源。
     */
    private <T> Optional<T> loadWithSingleflight(String key, Supplier<Optional<T>> loader,
                                                 java.util.function.Function<String, Optional<T>> cacheReader) {
        // 快路径：缓存命中直接返回
        Optional<T> cached = cacheReader.apply(key);
        if (cached != null) {
            return cached;
        }
        Object lock = rebuildLocks.computeIfAbsent(key, k -> new Object());
        try {
            synchronized (lock) {
                // 双查：等锁期间首个线程可能已回填缓存
                Optional<T> second = cacheReader.apply(key);
                if (second != null) {
                    return second;
                }
                return loader.get();
            }
        } finally {
            // 条件移除：仅当锁对象未被替换时清理，避免误删他人的锁
            rebuildLocks.remove(key, lock);
        }
    }

    public <T> Optional<T> getOrLoad(String key, Duration ttl, Supplier<Optional<T>> loader, Class<T> clazz) {
        Optional<T> result = loadWithSingleflight(key, loader, k -> {
            try {
                String cached = redisTemplate.opsForValue().get(k);
                if (cached != null) {
                    if (NULL_PLACEHOLDER.equals(cached)) {
                        return Optional.empty();
                    }
                    return Optional.of(objectMapper.readValue(cached, clazz));
                }
            } catch (Exception e) {
                log.warn("缓存读取失败，降级查库: key={}", k, e);
            }
            return null;
        });

        try {
            if (result.isPresent()) {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result.get()), randomizeTtl(ttl));
            } else {
                redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, Duration.ofMinutes(5));
            }
        } catch (Exception e) {
            log.warn("缓存回写失败: key={}", key, e);
        }

        return result;
    }

    public <T> Optional<T> getOrLoad(String key, Duration ttl, Supplier<Optional<T>> loader, TypeReference<T> typeRef) {
        Optional<T> result = loadWithSingleflight(key, loader, k -> {
            try {
                String cached = redisTemplate.opsForValue().get(k);
                if (cached != null) {
                    if (NULL_PLACEHOLDER.equals(cached)) {
                        return Optional.empty();
                    }
                    return Optional.of(objectMapper.readValue(cached, typeRef));
                }
            } catch (Exception e) {
                log.warn("缓存读取失败，降级查库: key={}", k, e);
            }
            return null;
        });

        try {
            if (result.isPresent()) {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result.get()), randomizeTtl(ttl));
            } else {
                redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, Duration.ofMinutes(5));
            }
        } catch (Exception e) {
            log.warn("缓存回写失败: key={}", key, e);
        }

        return result;
    }

    public <T> void put(String key, T value, Duration ttl) {
        try {
            if (value != null) {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), randomizeTtl(ttl));
            }
        } catch (Exception e) {
            log.warn("缓存写入失败: key={}", key, e);
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("缓存删除失败: key={}", key, e);
        }
    }

    public void evictByPrefix(String prefix) {
        try {
            List<String> batch = new ArrayList<>();
            Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(prefix + "*", CACHE_SCAN_COUNT);
            for (String key : keys) {
                batch.add(key);
                if (batch.size() >= CACHE_SCAN_COUNT) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
        } catch (Exception e) {
            log.warn("批量缓存删除失败: prefix={}", prefix, e);
        }
    }

    public void increment(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("计数器自增失败: key={}", key, e);
        }
    }

    public long getCount(String key) {
        try {
            String val = redisTemplate.opsForValue().get(key);
            if (val == null) {
                return 0;
            }
            return Long.parseLong(val);
        } catch (Exception e) {
            log.warn("计数器读取失败: key={}", key, e);
            return 0;
        }
    }

    public long incrementView(Long articleId) {
        String key = viewKey(articleId);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null) {
                redisTemplate.expire(key, VIEW_COUNTER_TTL);
                redisTemplate.opsForSet().add(VIEW_DIRTY_SET, String.valueOf(articleId));
                return count;
            }
        } catch (Exception e) {
            log.warn("浏览量计数失败: articleId={}", articleId, e);
        }
        return 0;
    }

    public long getPendingViewCount(Long articleId) {
        return getCount(viewKey(articleId));
    }

    public long drainViewCounter(Long articleId) {
        String key = viewKey(articleId);
        try {
            String value = redisTemplate.opsForValue().getAndDelete(key);
            redisTemplate.opsForSet().remove(VIEW_DIRTY_SET, String.valueOf(articleId));
            if (value == null) {
                return 0;
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            log.warn("浏览量计数清零失败: articleId={}", articleId, e);
            return 0;
        }
    }

    public void restoreViewCounter(Long articleId, long amount) {
        if (articleId == null || amount <= 0) {
            return;
        }
        String key = viewKey(articleId);
        try {
            redisTemplate.opsForValue().increment(key, amount);
            redisTemplate.expire(key, VIEW_COUNTER_TTL);
            redisTemplate.opsForSet().add(VIEW_DIRTY_SET, String.valueOf(articleId));
        } catch (Exception e) {
            log.warn("恢复浏览量增量失败: articleId={}, amount={}", articleId, amount, e);
        }
    }

    public List<Long> popPendingViewArticleIds(int max) {
        List<Long> articleIds = new ArrayList<>();
        try {
            for (int i = 0; i < max; i++) {
                String value = redisTemplate.opsForSet().pop(VIEW_DIRTY_SET);
                if (value == null) {
                    break;
                }
                try {
                    articleIds.add(Long.parseLong(value));
                } catch (NumberFormatException ignored) {
                    log.warn("脏浏览计数键格式错误: {}", value);
                }
            }
        } catch (Exception e) {
            log.warn("读取待同步文章失败", e);
        }
        return articleIds;
    }

    private String viewKey(Long articleId) {
        return VIEW_COUNTER_PREFIX + articleId;
    }

    private Duration randomizeTtl(Duration base) {
        long baseSeconds = base.getSeconds();
        long jitter = baseSeconds / 10;
        long randomized = baseSeconds + ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Duration.ofSeconds(Math.max(baseSeconds / 2, randomized));
    }
}
