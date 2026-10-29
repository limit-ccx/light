package com.zhiguang.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.AgentConfig;
import com.zhiguang.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 编排核心：SSE 流式 tool-loop。
 * <p>
 * 事件协议（设计文档 §4.6）：
 * status{stage} → tool{name,status}* → delta{text}* → done{conversationId,quotaLeft,truncated} / error{code,message}
 * <p>
 * 每轮以 stream=true 调 LLM：内容增量实时转发；tool_calls 增量按 index 装配，
 * 结束后经白名单注册表执行（限时不超 3s），结果回注进入下一轮，直到产出最终回答。
 * 历史只持久化 user/assistant 文本轮次（工具明细不进上下文）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final AgentConfig agentConfig;
    private final com.zhiguang.config.AiConfig aiConfig;
    private final WebClient aiWebClient;
    private final AgentToolRegistry toolRegistry;
    private final AgentSessionService sessionService;
    private final com.zhiguang.service.AiQuotaService aiQuotaService;
    private final com.zhiguang.service.AiMetrics aiMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Agent 专用有界线程池：与 Tomcat 工作线程隔离，池满由 Controller 快速拒绝 */
    private final ExecutorService agentExecutor = new ThreadPoolExecutor(
            4, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            r -> {
                Thread t = new Thread(r, "agent-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy());

    private record StreamResult(String content, List<Map<String, Object>> toolCalls,
                                String finishReason, Integer promptTokens, Integer completionTokens) {
    }

    public void streamChat(User user, String message, SseEmitter emitter) {
        long start = System.currentTimeMillis();
        String result = "success";
        AtomicBoolean clientGone = new AtomicBoolean(false);
        AtomicInteger promptTokens = new AtomicInteger(0);
        AtomicInteger completionTokens = new AtomicInteger(0);

        AgentSessionService.Session session = sessionService.loadOrCreate(user.getId());
        String conversationId = session.getConversationId();

        // LLM 消息：system + 历史 + 本轮用户消息
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", agentConfig.getSystemPrompt()));
        for (Map<String, String> turn : session.getMessages()) {
            messages.add(Map.of("role", turn.get("role"), "content", turn.get("content")));
        }
        messages.add(Map.of("role", "user", "content", message));

        try {
            sendSafely(emitter, clientGone, "status", Map.of("stage", "thinking"));

            String finalAnswer = null;
            boolean truncated = false;

            for (int iteration = 1; iteration <= agentConfig.getMaxIterations(); iteration++) {
                StreamResult round = streamRound(user.getId(), messages, emitter, clientGone,
                        promptTokens, completionTokens);

                if (round.toolCalls() != null && !round.toolCalls().isEmpty()) {
                    // 模型请求调用工具：执行后回注，进入下一轮
                    messages.add(assistantMessageWithToolCalls(round.content(), round.toolCalls()));
                    for (Map<String, Object> toolCall : round.toolCalls()) {
                        String toolName = String.valueOf(((Map<?, ?>) toolCall.get("function")).get("name"));
                        String arguments = String.valueOf(((Map<?, ?>) toolCall.get("function")).get("arguments"));
                        sendSafely(emitter, clientGone, "tool", Map.of("name", toolName, "status", "running"));
                        String toolResult = toolRegistry.execute(user.getId(), toolName, arguments);
                        sendSafely(emitter, clientGone, "tool", Map.of("name", toolName, "status", "done"));
                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", String.valueOf(toolCall.get("id")),
                                "content", toolResult));
                    }
                    continue;
                }

                finalAnswer = round.content();
                truncated = "length".equals(round.finishReason());
                if (iteration == agentConfig.getMaxIterations() && truncated) {
                    finalAnswer = finalAnswer == null ? "" : finalAnswer + "\n（回答因长度限制被截断）";
                }
                break;
            }

            if (finalAnswer == null) {
                // 迭代上限仍未收敛：给出兜底收尾
                finalAnswer = "这个问题我需要再多想一下，你可以换个问法，或先去相关功能里亲手体验一下～";
            }

            if (!clientGone.get()) {
                long quotaLeft = aiQuotaService.getRemaining(user.getId());
                sendSafely(emitter, clientGone, "done", Map.of(
                        "conversationId", conversationId,
                        "quotaLeft", quotaLeft,
                        "truncated", truncated));
            }

            sessionService.appendTurn(user.getId(), session, message, finalAnswer);
        } catch (Exception e) {
            if (clientGone.get()) {
                result = "cancelled";
                log.info("Agent 对话客户端已断开: userId={}", user.getId());
            } else {
                result = "error";
                log.error("Agent 对话失败: userId={}", user.getId(), e);
                sendSafely(emitter, clientGone, "error",
                        Map.of("code", "INTERNAL", "message", "助手暂时不可用，请稍后重试"));
            }
        } finally {
            aiMetrics.recordCall("agent-chat", result, System.currentTimeMillis() - start);
            if (promptTokens.get() > 0 || completionTokens.get() > 0) {
                aiMetrics.recordTokens("agent-chat",
                        promptTokens.get() > 0 ? promptTokens.get() : null,
                        completionTokens.get() > 0 ? completionTokens.get() : null);
            }
            try {
                emitter.complete();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 单轮流式调用：内容增量实时转发为 delta 事件；tool_calls 增量按 index 装配。
     */
    private StreamResult streamRound(Long userId, List<Map<String, Object>> messages, SseEmitter emitter,
                                     AtomicBoolean clientGone,
                                     AtomicInteger promptTokens, AtomicInteger completionTokens) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("tools", toolRegistry.toToolDefinitions());
        requestBody.put("tool_choice", "auto");
        requestBody.put("temperature", agentConfig.getTemperature());
        requestBody.put("max_tokens", agentConfig.getReplyMaxTokens());
        requestBody.put("stream", true);
        requestBody.put("stream_options", Map.of("include_usage", true));

        StringBuilder content = new StringBuilder();
        TreeMap<Integer, Map<String, Object>> toolCallsByIndex = new TreeMap<>();
        AtomicReference<String> finishReason = new AtomicReference<>("stop");
        boolean[] truncated = {false};

        aiWebClient.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(agentConfig.getRequestTimeoutSeconds()))
                .doOnNext(chunk -> handleChunk(chunk, emitter, clientGone, content, toolCallsByIndex,
                        finishReason, truncated, promptTokens, completionTokens))
                .blockLast();

        List<Map<String, Object>> toolCalls = toolCallsByIndex.values().stream().toList();
        return new StreamResult(content.toString(), toolCalls, finishReason.get(),
                promptTokens.get() > 0 ? promptTokens.get() : null,
                completionTokens.get() > 0 ? completionTokens.get() : null);
    }

    @SuppressWarnings("unchecked")
    private void handleChunk(String chunk, SseEmitter emitter, AtomicBoolean clientGone,
                             StringBuilder content, TreeMap<Integer, Map<String, Object>> toolCallsByIndex,
                             AtomicReference<String> finishReason, boolean[] truncated,
                             AtomicInteger promptTokens, AtomicInteger completionTokens) {
        try {
            String data = chunk == null ? "" : chunk.trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                return;
            }
            JsonNode node = objectMapper.readTree(data);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode choice = choices.get(0);
                String fr = choice.path("finish_reason").asText("");
                if (!fr.isEmpty()) {
                    finishReason.set(fr);
                    if ("length".equals(fr)) {
                        truncated[0] = true;
                    }
                }
                JsonNode delta = choice.path("delta");
                String piece = delta.path("content").asText("");
                if (!piece.isEmpty()) {
                    content.append(piece);
                    if (!sendSafely(emitter, clientGone, "delta", Map.of("text", piece))) {
                        throw new IllegalStateException("client gone");
                    }
                }
                JsonNode deltaToolCalls = delta.path("tool_calls");
                if (deltaToolCalls.isArray()) {
                    for (JsonNode tc : deltaToolCalls) {
                        int index = tc.path("index").asInt(0);
                        Map<String, Object> entry = toolCallsByIndex.computeIfAbsent(index,
                                k -> new LinkedHashMap<>(Map.of("id", "", "type", "function",
                                        "function", new LinkedHashMap<>(Map.of("name", "", "arguments", "")))));
                        if (tc.hasNonNull("id")) {
                            entry.put("id", tc.get("id").asText());
                        }
                        JsonNode fn = tc.path("function");
                        if (fn.hasNonNull("name")) {
                            ((Map<String, Object>) entry.get("function")).put("name", fn.get("name").asText());
                        }
                        if (fn.hasNonNull("arguments")) {
                            Map<String, Object> fnMap = (Map<String, Object>) entry.get("function");
                            fnMap.put("arguments", fnMap.get("arguments") + fn.get("arguments").asText());
                        }
                    }
                }
            }
            JsonNode usage = node.path("usage");
            if (usage.isObject()) {
                if (usage.hasNonNull("prompt_tokens")) {
                    promptTokens.set(usage.path("prompt_tokens").asInt());
                }
                if (usage.hasNonNull("completion_tokens")) {
                    completionTokens.set(usage.path("completion_tokens").asInt());
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Agent 解析流式 chunk 失败，跳过该块: {}", chunk, e);
        }
    }

    private Map<String, Object> assistantMessageWithToolCalls(String content, List<Map<String, Object>> toolCalls) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content == null ? "" : content);
        message.put("tool_calls", toolCalls);
        return message;
    }

    private boolean sendSafely(SseEmitter emitter, AtomicBoolean clientGone, String event, Object payload) {
        if (clientGone.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(payload));
            return true;
        } catch (Exception e) {
            clientGone.set(true);
            return false;
        }
    }

    public ExecutorService getExecutor() {
        return agentExecutor;
    }
}
