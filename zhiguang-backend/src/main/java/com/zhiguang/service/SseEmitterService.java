package com.zhiguang.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理
 * <p>
 * 以 "userId:channel" 为 key：同一用户的通知流（notification）与私信流（message）
 * 是两条独立连接，此前仅按 userId 存储会导致两条流互相 complete 对方，
 * EventSource 自动重连后形成"互踢-重连"风暴。
 */
@Slf4j
@Service
public class SseEmitterService {

    public static final String CHANNEL_NOTIFICATION = "notification";
    public static final String CHANNEL_MESSAGE = "message";

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L;
    private static final long HEARTBEAT_INTERVAL = 30 * 1000L;

    public SseEmitter subscribe(Long userId, String channel) {
        String key = channelKey(userId, channel);
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        SseEmitter previous = emitters.put(key, emitter);
        if (previous != null) {
            previous.complete();
        }

        emitter.onCompletion(() -> emitters.remove(key, emitter));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for {}", key);
            emitters.remove(key, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE error for {}: {}", key, e.getMessage());
            emitters.remove(key, emitter);
            emitter.complete();
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(Map.of("type", "CONNECT", "userId", userId, "channel", channel)));
        } catch (IOException e) {
            log.warn("Failed to send SSE connect event to {}, completing emitter", key, e);
            emitters.remove(key, emitter);
            emitter.complete();
        }

        return emitter;
    }

    public void sendToUser(Long userId, String channel, Object data) {
        SseEmitter emitter = emitters.get(channelKey(userId, channel));
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("message").data(data));
        } catch (IOException e) {
            log.warn("Failed to send SSE event to {}, completing emitter", channelKey(userId, channel), e);
            emitters.remove(channelKey(userId, channel), emitter);
            emitter.complete();
        }
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void heartbeat() {
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("type", "HEARTBEAT", "timestamp", System.currentTimeMillis())));
            } catch (IOException e) {
                log.warn("Failed to send SSE heartbeat to {}, completing emitter", key, e);
                emitters.remove(key, emitter);
                emitter.complete();
            }
        });
    }

    private String channelKey(Long userId, String channel) {
        return userId + ":" + channel;
    }
}
