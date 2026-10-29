package com.zhiguang.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 生成中断注册表：同一管理员同一时刻只有一条进行中的生成，
 * "停止"按钮置其取消标志，引擎在最近安全点（轮间/流中）收敛收尾。
 */
@Component
public class AgentAbortRegistry {

    private final Map<Long, AtomicBoolean> flags = new ConcurrentHashMap<>();

    public AtomicBoolean register(Long adminId) {
        return flags.computeIfAbsent(adminId, k -> new AtomicBoolean(false));
    }

    public void unregister(Long adminId) {
        flags.remove(adminId);
    }

    public boolean abort(Long adminId) {
        AtomicBoolean flag = flags.get(adminId);
        return flag != null && flag.compareAndSet(false, true);
    }
}
