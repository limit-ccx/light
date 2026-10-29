package com.zhiguang.controller;

import com.zhiguang.config.AgentConfig;
import com.zhiguang.dto.AgentChatRequestDto;
import com.zhiguang.dto.ApiResponse;
import com.zhiguang.entity.AgentFeedback;
import com.zhiguang.entity.User;
import com.zhiguang.repository.AgentFeedbackRepository;
import com.zhiguang.service.AiQuotaService;
import com.zhiguang.service.agent.AgentChatService;
import com.zhiguang.service.agent.AgentSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * 导览助手接口（C 端）。
 * <p>
 * 事件协议见 AgentChatService；配额与润色/写作助手等 AI 功能共享（AiQuotaService）。
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentChatService agentChatService;
    private final AgentSessionService sessionService;
    private final AgentConfig agentConfig;
    private final AiQuotaService aiQuotaService;
    private final AgentFeedbackRepository feedbackRepository;

    /** 发起对话（SSE 流式） */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AgentChatRequestDto request,
                           @AuthenticationPrincipal User user) {
        if (user == null) {
            return emitterWithError("UNAUTHORIZED", "请先登录后再使用助手");
        }
        if (!agentConfig.isEnabled()) {
            return emitterWithError("AGENT_DISABLED", "助手暂未开放，敬请期待");
        }
        if (request.getMessage().length() > agentConfig.getMaxInputChars()) {
            return emitterWithError("INPUT_TOO_LONG",
                    "单条消息不能超过 " + agentConfig.getMaxInputChars() + " 字");
        }
        if (!aiQuotaService.tryConsume(user.getId())) {
            return emitterWithError("QUOTA_EXCEEDED", "今日 AI 使用次数已用完，请明天再试");
        }

        SseEmitter emitter = new SseEmitter(agentConfig.getRequestTimeoutSeconds() * 1000L + 5_000L);
        try {
            agentChatService.streamChat(user, request.getMessage(), emitter);
        } catch (RejectedExecutionException e) {
            log.warn("Agent 线程池已满，拒绝本次请求");
            return emitterWithError(emitter, "BUSY", "当前使用人数较多，请稍后重试");
        }
        return emitter;
    }

    /** 恢复当前会话历史（刷新页面后） */
    @GetMapping("/conversations/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> currentConversation(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        AgentSessionService.Session session = sessionService.load(user.getId());
        if (session == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("conversationId", "", "messages", List.of())));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "conversationId", session.getConversationId(),
                "messages", session.getMessages())));
    }

    /** 清空当前会话（新对话） */
    @DeleteMapping("/conversations/current")
    public ResponseEntity<ApiResponse<Void>> clearConversation(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        sessionService.clear(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 今日剩余 AI 配额 */
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<Map<String, Long>>> quota(@AuthenticationPrincipal User user) {
        long remaining = user == null ? 0 : aiQuotaService.getRemaining(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("remaining", remaining)));
    }

    /** 助手可用状态（前端决定悬浮球入口显隐） */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(@AuthenticationPrincipal User user) {
        boolean loggedIn = user != null;
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "enabled", agentConfig.isEnabled(),
                "loggedIn", loggedIn)));
    }

    /** 回复反馈（赞/踩） */
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Void>> feedback(@RequestBody Map<String, String> body,
                                                      @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        String rating = body.getOrDefault("rating", "");
        if (!"LIKE".equals(rating) && !"DISLIKE".equals(rating)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "无效的反馈类型"));
        }
        AgentFeedback feedback = new AgentFeedback();
        feedback.setUserId(user.getId());
        feedback.setConversationId(trim(body.get("conversationId"), 64));
        feedback.setMessageId(trim(body.get("messageId"), 64));
        feedback.setRating(rating);
        feedback.setComment(trim(body.get("comment"), 500));
        feedbackRepository.save(feedback);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    private SseEmitter emitterWithError(String code, String message) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("code", code, "message", message)));
            emitter.complete();
        } catch (Exception ignore) {
        }
        return emitter;
    }

    private SseEmitter emitterWithError(SseEmitter emitter, String code, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("code", code, "message", message)));
            emitter.complete();
        } catch (Exception ignore) {
        }
        return emitter;
    }
}
