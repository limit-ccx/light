package com.zhiguang.service.agent.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.AdminAgentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端 Agent 会话（§12 上下文的 M1 层）。
 * <p>
 * Key: admin-agent:session:{adminId}，值 {conversationId, messages:[{role,content}], notes:[]}
 * notes 为系统通知（操作确认结果等），下一轮组装时注入（§5.2 回注）。
 */
@Service
@RequiredArgsConstructor
public class AdminAgentSessionService {

    private static final String KEY_PREFIX = "admin-agent:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminAgentConfig config;

    public static class Session {
        private String conversationId;
        private List<Map<String, String>> messages = new ArrayList<>();
        private List<String> notes = new ArrayList<>();

        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public List<Map<String, String>> getMessages() { return messages; }
        public void setMessages(List<Map<String, String>> messages) { this.messages = messages; }
        public List<String> getNotes() { return notes; }
        public void setNotes(List<String> notes) { this.notes = notes; }
    }

    public Session loadOrCreate(Long adminId) {
        Session session = load(adminId);
        if (session == null) {
            session = new Session();
            session.setConversationId(UUID.randomUUID().toString());
        }
        return session;
    }

    public Session load(Long adminId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + adminId);
            return json == null ? null : objectMapper.readValue(json, Session.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void appendTurn(Long adminId, Session session, String userMessage, String assistantMessage) {
        session.getMessages().add(Map.of("role", "user", "content", userMessage));
        if (assistantMessage != null && !assistantMessage.isBlank()) {
            session.getMessages().add(Map.of("role", "assistant", "content", assistantMessage));
        }
        int keep = config.getHistoryKeeps();
        if (session.getMessages().size() > keep) {
            session.setMessages(new ArrayList<>(session.getMessages().subList(session.getMessages().size() - keep, session.getMessages().size())));
        }
        save(adminId, session);
    }

    /** 系统通知（操作确认结果回注，§5.2） */
    public void addNote(Long adminId, String note) {
        Session session = loadOrCreate(adminId);
        session.getNotes().add(note);
        if (session.getNotes().size() > 10) {
            session.setNotes(new ArrayList<>(session.getNotes().subList(session.getNotes().size() - 10, session.getNotes().size())));
        }
        save(adminId, session);
    }

    /** 取走并清空 notes（组装上下文时消费） */
    public List<String> drainNotes(Long adminId, Session session) {
        List<String> notes = new ArrayList<>(session.getNotes() == null ? List.of() : session.getNotes());
        session.setNotes(new ArrayList<>());
        return notes;
    }

    public void save(Long adminId, Session session) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + adminId,
                    objectMapper.writeValueAsString(session), Duration.ofHours(24));
        } catch (Exception ignored) {
        }
    }

    public void clear(Long adminId) {
        try {
            redisTemplate.delete(KEY_PREFIX + adminId);
        } catch (Exception ignored) {
        }
    }
}
