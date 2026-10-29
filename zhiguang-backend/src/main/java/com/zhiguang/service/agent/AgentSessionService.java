package com.zhiguang.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 会话历史（Redis）。
 * <p>
 * Key: agent:session:{userId}，值 {conversationId, messages:[{role, content}]}，
 * TTL 24h（每次对话续期）；超过 history-keeps 裁剪最旧消息。
 * 只持久化 user/assistant 文本轮次，工具调用明细不进历史。
 */
@Service
@RequiredArgsConstructor
public class AgentSessionService {

    private static final String KEY_PREFIX = "agent:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final com.zhiguang.config.AgentConfig agentConfig;

    public static class Session {
        private String conversationId;
        private List<Map<String, String>> messages = new ArrayList<>();

        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public List<Map<String, String>> getMessages() { return messages; }
        public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }
    }

    /** 读取当前会话；不存在则新建（新 conversationId） */
    public Session loadOrCreate(Long userId) {
        Session session = load(userId);
        if (session == null) {
            session = new Session();
            session.setConversationId(UUID.randomUUID().toString());
        }
        return session;
    }

    public Session load(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, Session.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 追加一轮 user/assistant 对话并保存（裁剪 + 续期） */
    public void appendTurn(Long userId, Session session, String userMessage, String assistantMessage) {
        session.getMessages().add(Map.of("role", "user", "content", userMessage));
        if (assistantMessage != null && !assistantMessage.isBlank()) {
            session.getMessages().add(Map.of("role", "assistant", "content", assistantMessage));
        }
        int keep = agentConfig.getHistoryKeeps();
        List<Map<String, String>> messages = session.getMessages();
        if (messages.size() > keep) {
            session.setMessages(new ArrayList<>(messages.subList(messages.size() - keep, messages.size())));
        }
        save(userId, session);
    }

    public void save(Long userId, Session session) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + userId,
                    objectMapper.writeValueAsString(session), Duration.ofHours(24));
        } catch (Exception e) {
            // 会话保存失败不阻断对话，仅影响跨请求上下文
        }
    }

    public void clear(Long userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception ignored) {
        }
    }
}
