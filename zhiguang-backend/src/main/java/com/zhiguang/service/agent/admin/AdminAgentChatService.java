package com.zhiguang.service.agent.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.AdminAgentConfig;
import com.zhiguang.config.AiConfig;
import com.zhiguang.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 管理员智能体 ReAct 编排引擎（设计方案 §2/§6/§12/§13）。
 * <p>
 * 事件协议：status / action{name,status} / observation{name,summary} / delta{text} /
 * action-draft{operationId,tool,args,riskLevel,reason,targetSummary} /
 * done{pendingActions,quotaLeft} / error{code,message}
 * <p>
 * 前端渲染约定：delta 按 action 事件分段——每个 action 之前的 delta 段为"思考"，
 * 最后一个 action 之后的 delta 段为最终回答。
 * <p>
 * LoopGuard（L1-L7）全部在本引擎内实施；上下文组装遵循 §12（时间锚定、
 * 不可信边界标记、观察瘦身）；防幻觉提示置于系统提示最高优先级（§13.1）。
 */
@Slf4j
@Service
public class AdminAgentChatService {

    private final AdminAgentConfig config;
    private final AiConfig aiConfig;
    private final WebClient aiWebClient;
    private final AdminToolRegistry registry;
    private final AdminAgentSessionService sessionService;
    private final AdminAgentActionService actionService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.zhiguang.service.AiMetrics aiMetrics;
    private final ThreadPoolTaskExecutor aiWorkerExecutor;
    private final org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;
    private final com.zhiguang.repository.AdminAgentChatLogRepository chatLogRepository;
    private final com.zhiguang.security.AgentAbortRegistry abortRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminAgentChatService(AdminAgentConfig config, AiConfig aiConfig, WebClient aiWebClient,
                                 AdminToolRegistry registry, AdminAgentSessionService sessionService,
                                 AdminAgentActionService actionService,
                                 org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                                 com.zhiguang.service.AiMetrics aiMetrics,
                                 @Qualifier("aiWorkerExecutor") ThreadPoolTaskExecutor aiWorkerExecutor,
                                 org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder,
                                 com.zhiguang.repository.AdminAgentChatLogRepository chatLogRepository,
                                 com.zhiguang.security.AgentAbortRegistry abortRegistry) {
        this.config = config;
        this.aiConfig = aiConfig;
        this.aiWebClient = aiWebClient;
        this.registry = registry;
        this.sessionService = sessionService;
        this.actionService = actionService;
        this.redisTemplate = redisTemplate;
        this.aiMetrics = aiMetrics;
        this.aiWorkerExecutor = aiWorkerExecutor;
        this.webClientBuilder = webClientBuilder;
        this.chatLogRepository = chatLogRepository;
        this.abortRegistry = abortRegistry;
    }

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private record StreamResult(String content, List<Map<String, Object>> toolCalls,
                                String finishReason, Integer promptTokens, Integer completionTokens) {
    }

    /** 可用平台配置（Controller /models 用） */
    public Map<String, AiConfig.ProviderSetting> availableProviders() {
        return aiConfig.getProviders();
    }

    /** 持久化对话历史 */
    public org.springframework.data.domain.Page<com.zhiguang.entity.AdminAgentChatLog> getChatHistory(
            Long adminId, int page, int size) {
        return chatLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId,
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    public void streamChat(User admin, String message, String provider, String effort, SseEmitter emitter) {
        AtomicBoolean abortFlag = abortRegistry.register(admin.getId());
        aiWorkerExecutor.execute(() -> {
            try {
                doStreamChat(admin, message, provider, effort, emitter, abortFlag);
            } finally {
                abortRegistry.unregister(admin.getId());
            }
        });
    }

    /** 管理员请求停止当前生成（前端"停止"按钮）：置取消标志，引擎在最近的安全点收敛收尾 */
    public boolean stop(Long adminId) {
        return abortRegistry.abort(adminId);
    }

    /** 解析 provider 配置（校验存在与密钥），返回 [providerSetting, 实际模型名] */
    private Object[] resolveProvider(String provider, String effort) {
        var providers = aiConfig.getProviders();
        String key = provider == null || provider.isBlank() ? "deepseek" : provider.toLowerCase();
        AiConfig.ProviderSetting setting = providers.get(key);
        if (setting == null) {
            throw new IllegalArgumentException("未知模型平台: " + key + "（可选: " + providers.keySet() + "）");
        }
        if (setting.getApiKey() == null || setting.getApiKey().isBlank()
                || setting.getApiKey().startsWith("${")) {
            throw new IllegalArgumentException("平台 " + key + " 未配置 API 密钥（缺少对应环境变量）");
        }
        boolean deep = "deep".equalsIgnoreCase(effort);
        String model = deep && setting.getReasoningModel() != null && !setting.getReasoningModel().isBlank()
                ? setting.getReasoningModel() : setting.getModel();
        return new Object[]{setting, model};
    }

    private void doStreamChat(User admin, String message, String provider, String effort,
                              SseEmitter emitter, AtomicBoolean abortFlag) {
        long start = System.currentTimeMillis();
        long deadline = start + config.getRequestTimeoutSeconds() * 1000L;
        String result = "success";
        AtomicBoolean clientGone = new AtomicBoolean(false);
        AtomicInteger promptTokensTotal = new AtomicInteger(0);

        AdminAgentSessionService.Session session = sessionService.loadOrCreate(admin.getId());

        // ===== 上下文组装（§12）=====
        List<String> notes = sessionService.drainNotes(admin.getId(), session);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                config.getSystemPrompt()
                        + "\n\n【运行时状态】当前时间：" + LocalDateTime.now().format(TIME_FMT)
                        + "（" + dayOfWeek() + "）。回答中的相对时间一律以此时点换算。"));
        for (Map<String, String> turn : session.getMessages()) {
            messages.add(Map.of("role", turn.get("role"), "content", turn.get("content")));
        }
        if (!notes.isEmpty()) {
            StringBuilder noteBlock = new StringBuilder("【系统通知（操作确认结果）】\n");
            for (String note : notes) {
                noteBlock.append("- ").append(note).append('\n');
            }
            messages.add(Map.of("role", "user", "content", noteBlock.toString()));
        }
        messages.add(Map.of("role", "user", "content", "<用户消息>" + message + "</用户消息>"));

        // ===== LoopGuard 状态 =====
        int iterations = 0;
        int toolCallCount = 0;
        int emptyStreak = 0;
        int draftsCount = (int) actionService.listPending(admin.getId()).size();
        Map<String, String> signatureCache = new HashMap<>();
        boolean toolsDisabled = false;

        AiConfig.ProviderSetting providerSetting;
        String actualModel;
        try {
            Object[] resolved = resolveProvider(provider, effort);
            providerSetting = (AiConfig.ProviderSetting) resolved[0];
            actualModel = (String) resolved[1];
        } catch (IllegalArgumentException e) {
            sendSafely(emitter, clientGone, "error", Map.of("code", "BAD_PROVIDER", "message", e.getMessage()));
            try {
                emitter.complete();
            } catch (Exception ignore) {
            }
            return;
        }

        try {
            sendSafely(emitter, clientGone, "status", Map.of("stage", "thinking"));

            String finalAnswer = null;
            List<AdminAgentActionService.PendingAction> drafts = new ArrayList<>();

            while (finalAnswer == null) {
                // L0 用户停止：收敛收尾（不抛错，保留已有内容）
                if (abortFlag.get()) {
                    finalAnswer = "（已按你的要求停止生成。以上是停止前的内容，可继续追问。）";
                    break;
                }
                // L1 / L5 检查
                if (iterations >= config.getMaxIterations()) {
                    finalAnswer = "本次任务步骤较多已截断。以上是我目前查到的信息与已生成的待确认操作，可确认执行或继续分批提问。";
                    break;
                }
                if (promptTokensTotal.get() > config.getTokenBudget()
                        || System.currentTimeMillis() > deadline) {
                    finalAnswer = "本次请求已达预算上限（上下文长度或时间）。请基于以上结果继续，或开启新任务。";
                    break;
                }

                iterations++;
                StreamResult round = streamRound(messages, !toolsDisabled, emitter, clientGone,
                        promptTokensTotal, providerSetting, actualModel, abortFlag);

                if (round.toolCalls() == null || round.toolCalls().isEmpty()) {
                    finalAnswer = round.content() == null || round.content().isBlank()
                            ? "（助手没有输出内容，请重试）" : round.content();
                    break;
                }

                // 工具调用轮：本轮 content 是思考文本，已作为 delta 推送（前端按 action 分段渲染为思考）
                messages.add(assistantMessageWithToolCalls(round.content(), round.toolCalls()));

                for (Map<String, Object> toolCall : round.toolCalls()) {
                    if (clientGone.get()) {
                        break;
                    }
                    toolCallCount++;
                    // L2 工具调用预算
                    if (toolCallCount > config.getMaxToolCalls()) {
                        injectObservation(messages, toolCall, "[工具结果] 已达单次任务工具调用上限，请基于已有信息直接回答。[/工具结果]");
                        continue;
                    }
                    String toolName = String.valueOf(((Map<?, ?>) toolCall.get("function")).get("name"));
                    String arguments = String.valueOf(((Map<?, ?>) toolCall.get("function")).get("arguments"));
                    AdminAgentTool tool = registry.get(toolName);
                    if (tool == null && !registry.isBuiltin(toolName)) {
                        injectObservation(messages, toolCall,
                                "[工具失败 tool=" + toolName + "] 该工具不存在（只能调用已注册的工具）[/工具失败]");
                        emptyStreak = bumpStreak(emptyStreak, "unknown tool");
                        continue;
                    }

                    // 征询工具：不执行任何逻辑，推 ask 事件给前端渲染征询卡片，本轮由模型收尾
                    if ("ask_user".equals(toolName)) {
                        Map<String, Object> askArgs = parseArgs(arguments);
                        sendSafely(emitter, clientGone, "action", Map.of("name", toolName, "status", "done"));
                        sendSafely(emitter, clientGone, "ask", Map.of(
                                "question", String.valueOf(askArgs.getOrDefault("question", "")),
                                "suggestion", String.valueOf(askArgs.getOrDefault("suggestion", "")),
                                "options", askArgs.getOrDefault("options", List.of())));
                        injectObservation(messages, toolCall,
                                "[工具结果] 已向管理员发出征询。本轮直接结束（不要生成操作，不要重复征询），"
                                        + "等待管理员下一条消息答复后再继续。[/工具结果]");
                        emptyStreak = 0;
                        continue;
                    }

                    sendSafely(emitter, clientGone, "action", Map.of("name", toolName, "status", "running"));

                    // 写操作：转 draft（§5.2），不执行
                    if (tool.write()) {
                        // L7 写操作上限
                        if (draftsCount >= config.getMaxDraftsPerRequest()) {
                            injectObservation(messages, toolCall,
                                    "[工具结果] 单次任务待确认操作已达上限（" + config.getMaxDraftsPerRequest()
                                            + " 个），请先让管理员确认，或分批处理。[/工具结果]");
                            sendSafely(emitter, clientGone, "action", Map.of("name", toolName, "status", "rejected"));
                            continue;
                        }
                        try {
                            Map<String, Object> args = parseArgs(arguments);
                            String reason = args.get("reason") == null
                                    ? (args.get("note") == null ? "" : String.valueOf(args.get("note")))
                                    : String.valueOf(args.get("reason"));
                            AdminAgentActionService.PendingAction draft =
                                    actionService.createDraft(admin.getId(), toolName, args, reason);
                            drafts.add(draft);
                            draftsCount++;
                            sendSafely(emitter, clientGone, "action-draft", draftPayload(draft));
                            injectObservation(messages, toolCall,
                                    "[工具结果] 操作已生成待确认卡片（operationId=" + draft.getOperationId()
                                            + "，摘要：" + draft.getTargetSummary()
                                            + "）。告知管理员需在界面上确认后才会执行。[/工具结果]");
                        } catch (Exception e) {
                            injectObservation(messages, toolCall,
                                    "[工具失败 tool=" + toolName + "] " + e.getMessage() + " [/工具失败]");
                            emptyStreak = bumpStreak(emptyStreak, "draft failed");
                        }
                        continue;
                    }

                    // 查询工具：L3 签名去重
                    String sig = registry.signature(toolName, arguments);
                    if (config.isDuplicateHint() && signatureCache.containsKey(sig)) {
                        injectObservation(messages, toolCall,
                                "[工具结果] 该查询本任务内已执行过，结果相同：" + signatureCache.get(sig)
                                        + "。请基于已有信息回答，不要重复调用。[/工具结果]");
                        sendSafely(emitter, clientGone, "action", Map.of("name", toolName, "status", "cached"));
                        continue;
                    }

                    String toolResult;
                    try {
                        toolResult = tool.execute(admin.getId(), parseArgs(arguments));
                    } catch (Exception e) {
                        toolResult = null;
                        injectObservation(messages, toolCall,
                                "[工具失败 tool=" + toolName + "] " + e.getMessage() + " [/工具失败]");
                    }
                    if (toolResult == null) {
                        emptyStreak = bumpStreak(emptyStreak, "tool failed");
                        continue;
                    }
                    emptyStreak = 0;
                    String trimmed = toolResult.length() > 1500
                            ? toolResult.substring(0, 1500) + "…(截断)" : toolResult; // §12.3 观察瘦身
                    signatureCache.put(sig, trimmed);
                    injectObservation(messages, toolCall,
                            "[工具结果 tool=" + toolName + "] " + trimmed + " [/工具结果]");
                    sendSafely(emitter, clientGone, "observation",
                            Map.of("name", toolName, "summary", trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed));
                    sendSafely(emitter, clientGone, "action", Map.of("name", toolName, "status", "done"));
                }

                // L4 空结果短路：移除 tools，模型只能基于已有信息作答（同时是防幻觉机制 §13.6）
                if (emptyStreak >= config.getEmptyShortCircuit() && !toolsDisabled) {
                    toolsDisabled = true;
                    messages.add(Map.of("role", "user", "content",
                            "【系统提示】工具已连续多次无有效结果，工具调用已停用。请基于已有信息直接回答；查不到的如实说明。"));
                }
            }

            if (!clientGone.get()) {
                String modelTag = provider.toLowerCase() + "/" + actualModel
                        + ("deep".equalsIgnoreCase(effort) ? "(deep)" : "");
                sessionService.appendTurn(admin.getId(), session, message, finalAnswer);
                try {
                    com.zhiguang.entity.AdminAgentChatLog userLog = new com.zhiguang.entity.AdminAgentChatLog();
                    userLog.setAdminId(admin.getId());
                    userLog.setRole("user");
                    userLog.setContent(message);
                    userLog.setModel(modelTag);
                    userLog.setEffort(effort);
                    chatLogRepository.save(userLog);
                    com.zhiguang.entity.AdminAgentChatLog aiLog = new com.zhiguang.entity.AdminAgentChatLog();
                    aiLog.setAdminId(admin.getId());
                    aiLog.setRole("assistant");
                    aiLog.setContent(finalAnswer);
                    aiLog.setModel(modelTag);
                    aiLog.setEffort(effort);
                    chatLogRepository.save(aiLog);
                } catch (Exception logEx) {
                    log.warn("Agent 历史落库失败: {}", logEx.getMessage());
                }
                sendSafely(emitter, clientGone, "done", Map.of(
                        "conversationId", session.getConversationId(),
                        "pendingActions", draftsCount,
                        "quotaLeft", quotaRemaining(admin.getId()),
                        "model", modelTag));
            }
        } catch (Exception e) {
            boolean aborted = abortFlag.get()
                    || (e.getMessage() != null && e.getMessage().contains("aborted by user"));
            if (clientGone.get()) {
                result = "cancelled";
            } else if (aborted) {
                result = "aborted";
                sendSafely(emitter, clientGone, "delta",
                        Map.of("text", "\n\n（已停止生成。）"));
            } else {
                result = "error";
                log.error("Admin Agent 对话失败: adminId={}", admin.getId(), e);
                sendSafely(emitter, clientGone, "error",
                        Map.of("code", "INTERNAL", "message", "助手暂时不可用，请稍后重试"));
            }
        } finally {
            aiMetrics.recordCall("admin-agent-chat", result, System.currentTimeMillis() - start);
            try {
                emitter.complete();
            } catch (Exception ignore) {
            }
        }
    }

    // ===== 单轮流式调用（与 C 端 AgentChatService 同骨架，含 tool_calls 增量装配） =====

    private StreamResult streamRound(List<Map<String, Object>> messages, boolean withTools,
                                     SseEmitter emitter, AtomicBoolean clientGone,
                                     AtomicInteger promptTokensTotal,
                                     AiConfig.ProviderSetting provider, String model,
                                     AtomicBoolean abortFlag) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        if (withTools) {
            requestBody.put("tools", registry.toToolDefinitions());
            requestBody.put("tool_choice", "auto");
        }
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getReplyMaxTokens());
        requestBody.put("stream", true);
        requestBody.put("stream_options", Map.of("include_usage", true));

        StringBuilder content = new StringBuilder();
        TreeMap<Integer, Map<String, Object>> toolCallsByIndex = new TreeMap<>();
        AtomicReference<String> finishReason = new AtomicReference<>("stop");

        WebClient client = webClientBuilder
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + provider.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        client.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(60))
                .doOnNext(chunk -> {
                    if (abortFlag.get()) {
                        // 用户停止：立刻断开上游 LLM 流（takeUntil 语义）
                        throw new IllegalStateException("aborted by user");
                    }
                    handleChunk(chunk, emitter, clientGone, content,
                            toolCallsByIndex, finishReason, promptTokensTotal);
                })
                .blockLast();

        return new StreamResult(content.toString(), List.copyOf(toolCallsByIndex.values()),
                finishReason.get(), null, null);
    }

    @SuppressWarnings("unchecked")
    private void handleChunk(String chunk, SseEmitter emitter, AtomicBoolean clientGone,
                             StringBuilder content, TreeMap<Integer, Map<String, Object>> toolCallsByIndex,
                             AtomicReference<String> finishReason, AtomicInteger promptTokensTotal) {
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
            if (usage.isObject() && usage.hasNonNull("prompt_tokens")) {
                promptTokensTotal.addAndGet(usage.path("prompt_tokens").asInt());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Admin Agent 解析流式 chunk 失败，跳过: {}", chunk, e);
        }
    }

    // ===== 辅助 =====

    private int bumpStreak(int streak, String reason) {
        log.debug("Admin Agent 空结果连续 +1: {}", reason);
        return streak + 1;
    }

    private Map<String, Object> assistantMessageWithToolCalls(String content, List<Map<String, Object>> toolCalls) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content == null ? "" : content);
        message.put("tool_calls", toolCalls);
        return message;
    }

    private void injectObservation(List<Map<String, Object>> messages, Map<String, Object> toolCall, String result) {
        messages.add(Map.of(
                "role", "tool",
                "tool_call_id", String.valueOf(toolCall.get("id")),
                "content", result));
    }

    private Map<String, Object> draftPayload(AdminAgentActionService.PendingAction draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operationId", draft.getOperationId());
        payload.put("tool", draft.getTool());
        payload.put("args", draft.getArgs());
        payload.put("riskLevel", draft.getRiskLevel());
        payload.put("reason", draft.getReason());
        payload.put("targetSummary", draft.getTargetSummary());
        return payload;
    }

    private Map<String, Object> parseArgs(String argumentsJson) throws Exception {
        if (argumentsJson == null || argumentsJson.isBlank() || "null".equals(argumentsJson)) {
            return Map.of();
        }
        return objectMapper.readValue(argumentsJson, Map.class);
    }

    private long quotaRemaining(Long adminId) {
        // 与 C 端 ai:quota 独立的 admin-agent 配额（§8）
        try {
            String key = "admin-agent:quota:" + adminId + ":" + java.time.LocalDate.now();
            String value = redisTemplate.opsForValue().get(key);
            long used = value == null ? 0 : Long.parseLong(value);
            return Math.max(0, config.getDailyQuota() - used);
        } catch (Exception e) {
            return -1;
        }
    }

    /** 配额计数（Controller 层在对话开始时调用） */
    public boolean tryConsumeQuota(Long adminId) {
        try {
            String key = "admin-agent:quota:" + adminId + ":" + java.time.LocalDate.now();
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofHours(25));
            }
            return count == null || count <= config.getDailyQuota();
        } catch (Exception e) {
            return true; // 配额组件故障不阻断
        }
    }

    private String dayOfWeek() {
        return switch (LocalDateTime.now().getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            default -> "周日";
        };
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
}
