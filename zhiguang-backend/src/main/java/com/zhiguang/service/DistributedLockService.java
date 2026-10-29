package com.zhiguang.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 * <p>
 * 基于 Redisson 提供分布式锁功能：
 * - 支持锁的自动续期（watchdog）
 * - 支持锁超时时间设置
 * - 支持可重入锁
 * - 提供便捷的函数式接口
 */
@Slf4j
@Service
public class DistributedLockService {

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 使用分布式锁执行任务（带超时时间）
     *
     * @param lockKey     锁的键
     * @param waitTime    获取锁的最大等待时间（秒）
     * @param leaseTime   锁的持有时间（秒）
     * @param task        需要在锁内执行的任务
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁失败: key={}, waitTime={}s", lockKey, waitTime);
                throw new RuntimeException("操作过于频繁，请稍后重试");
            }
            
            log.debug("获取分布式锁成功: key={}, leaseTime={}s", lockKey, leaseTime);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: key={}", lockKey, e);
            throw new RuntimeException("操作被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁: key={}", lockKey);
            }
        }
    }

    /**
     * 使用分布式锁执行任务（无返回值）
     *
     * @param lockKey     锁的键
     * @param waitTime    获取锁的最大等待时间（秒）
     * @param leaseTime   锁的持有时间（秒）
     * @param task        需要在锁内执行的任务
     */
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable task) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 使用分布式锁执行任务（自动续期）
     * <p>
     * 不设置 leaseTime，Redisson 会自动续期（watchdog 机制）
     *
     * @param lockKey  锁的键
     * @param waitTime 获取锁的最大等待时间（秒）
     * @param task     需要在锁内执行的任务
     * @return 任务执行结果
     */
    public <T> T executeWithLockWatchdog(String lockKey, long waitTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁失败（watchdog模式）: key={}, waitTime={}s", lockKey, waitTime);
                throw new RuntimeException("操作过于频繁，请稍后重试");
            }
            
            log.debug("获取分布式锁成功（watchdog模式）: key={}", lockKey);
            return task.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: key={}", lockKey, e);
            throw new RuntimeException("操作被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁: key={}", lockKey);
            }
        }
    }

    /**
     * 尝试获取锁（不执行任务）
     *
     * @param lockKey   锁的键
     * @param waitTime  获取锁的最大等待时间（秒）
     * @param leaseTime 锁的持有时间（秒）
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            log.debug("尝试获取分布式锁: key={}, result={}", lockKey, acquired);
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的键
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放分布式锁: key={}", lockKey);
        } else {
            log.warn("尝试释放不属于当前线程的锁: key={}", lockKey);
        }
    }

    /**
     * 检查锁是否被当前线程持有
     *
     * @param lockKey 锁的键
     * @return 是否被当前线程持有
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 检查锁是否被任何线程持有
     *
     * @param lockKey 锁的键
     * @return 是否被持有
     */
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}