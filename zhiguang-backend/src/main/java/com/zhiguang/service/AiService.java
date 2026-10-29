package com.zhiguang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.AiConfig;
import com.zhiguang.dto.AiAssistResponseDto;
import com.zhiguang.dto.PolishRequestDto;
import com.zhiguang.dto.PolishResponseDto;
import com.zhiguang.exception.AiOutputTruncatedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiConfig aiConfig;
    private final WebClient aiWebClient;
    private final org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;
    private final CacheService cacheService;
    private final AiMetrics aiMetrics;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 并发 LLM 调用上限（信号量）：同步 AI 端点会占住 Tomcat 工作线程等待响应，
     * 不设上限时约 200 个并发 AI 请求即可耗尽默认线程池导致整站不可用。
     * 超出上限的请求快速失败（"人数较多"），而非排队 60 秒。
     */
    private final java.util.concurrent.Semaphore llmCallSlots = new java.util.concurrent.Semaphore(48, true);

    /** 结果缓存：相同 内容+模式+模型 一小时内不重复计费 */
    private static final String CACHE_KEY_PREFIX = "ai:cache:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * 流式润色专用线程池（有界）：与 Tomcat 工作线程隔离，
     * 避免长时间 LLM 流式回写占用 Servlet 线程导致整站线程耗尽。
     */
    private final ExecutorService aiStreamExecutor = new ThreadPoolExecutor(
            4, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new ThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    private static class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger seq = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "ai-stream-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }

    /** 一次 LLM 调用的产出：content + token 用量 */
    private record ChatOutcome(String content, Integer promptTokens, Integer completionTokens) {
    }

    // ========== AI 润色 ==========

    /**
     * 润色文章内容（同步全量返回；前端推荐走 {@link #streamPolishContent}）
     */
    public PolishResponseDto polishContent(PolishRequestDto request) {
        String mode = (request.getMode() != null && !request.getMode().isBlank())
                ? request.getMode() : "polish";
        AiConfig.PromptSetting setting = resolvePromptSetting(mode);

        String polished = cachedString("polish", request.getContent() + "|" + mode, request.getTitle(),
                () -> {
                    String userMessage = buildPolishUserMessage(request);
                    Map<String, Object> requestBody = chatRequestBody(
                            setting.getPrompt(), userMessage, setting.getTemperature(), setting.getMaxTokens());
                    return callChat("polish", requestBody).content();
                });

        log.info("AI 润色完成，模式: {}，润色后内容长度: {}", mode, polished.length());
        return new PolishResponseDto(request.getContent(), polished, mode, aiConfig.getModel());
    }

    private String buildPolishUserMessage(PolishRequestDto request) {
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            return "文章标题：" + request.getTitle() + "\n\n文章正文：\n" + request.getContent();
        }
        return request.getContent();
    }

    private AiConfig.PromptSetting resolvePromptSetting(String mode) {
        AiConfig.PromptSetting setting = aiConfig.getPrompts().get(mode);
        return setting != null ? setting : aiConfig.getPrompts().get("polish");
    }

    /**
     * 润色文章内容（SSE 流式）。在独立有界线程池执行，事件协议：
     * status(thinking) → delta(text)* → done{model,truncated} / error{code,message}
     */
    public void streamPolishContent(PolishRequestDto request, SseEmitter emitter) {
        String mode = (request.getMode() != null && !request.getMode().isBlank())
                ? request.getMode() : "polish";
        AiConfig.PromptSetting setting = resolvePromptSetting(mode);

        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", setting.getPrompt()),
                        Map.of("role", "user", "content", buildPolishUserMessage(request))
                ),
                "temperature", setting.getTemperature(),
                "max_tokens", setting.getMaxTokens(),
                "stream", true,
                "stream_options", Map.of("include_usage", true)
        );

        aiStreamExecutor.execute(() -> doStreamPolish(requestBody, emitter));
    }

    private void doStreamPolish(Map<String, Object> requestBody, SseEmitter emitter) {
        long start = System.currentTimeMillis();
        StringBuilder full = new StringBuilder();
        AtomicBoolean clientGone = new AtomicBoolean(false);
        AtomicReference<Integer> promptTokens = new AtomicReference<>();
        AtomicReference<Integer> completionTokens = new AtomicReference<>();
        AtomicBoolean truncated = new AtomicBoolean(false);
        String result = "success";

        try {
            sendSafely(emitter, clientGone, "status", Map.of("stage", "thinking"));

            aiWebClient.post()
                    .uri("/chat/completions")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds() + 30L))
                    .doOnNext(chunk -> handleStreamChunk(
                            chunk, emitter, clientGone, full, truncated, promptTokens, completionTokens))
                    .blockLast();

            if (!clientGone.get()) {
                if (truncated.get()) {
                    log.warn("AI 流式润色输出被截断（finish_reason=length），长度: {}", full.length());
                }
                sendSafely(emitter, clientGone, "done", Map.of(
                        "model", aiConfig.getModel(),
                        "length", full.length(),
                        "truncated", truncated.get()));
            }
        } catch (Exception e) {
            if (clientGone.get()) {
                result = "cancelled";
                log.info("AI 流式润色客户端已断开，终止生成");
            } else {
                result = "error";
                log.error("AI 流式润色失败", e);
                sendSafely(emitter, clientGone, "error",
                        Map.of("code", "INTERNAL", "message", "AI 服务暂时不可用，请稍后重试"));
            }
        } finally {
            aiMetrics.recordCall("polish-stream", result, System.currentTimeMillis() - start);
            aiMetrics.recordTokens("polish-stream", promptTokens.get(), completionTokens.get());
            try {
                emitter.complete();
            } catch (Exception ignore) {
            }
        }
    }

    /** 解析上游 SSE chunk 并转发 delta 事件；客户端断开时抛异常终止上游流 */
    private void handleStreamChunk(String chunk, SseEmitter emitter, AtomicBoolean clientGone,
                                   StringBuilder full, AtomicBoolean truncated,
                                   AtomicReference<Integer> promptTokens,
                                   AtomicReference<Integer> completionTokens) {
        try {
            String data = chunk == null ? "" : chunk.trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                return;
            }
            JsonNode node = objectMapper.readTree(data);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode choice = choices.get(0);
                String piece = choice.path("delta").path("content").asText("");
                if (!piece.isEmpty()) {
                    full.append(piece);
                    if (!sendSafely(emitter, clientGone, "delta", Map.of("text", piece))) {
                        throw new IllegalStateException("client gone");
                    }
                }
                if ("length".equals(choice.path("finish_reason").asText(""))) {
                    truncated.set(true);
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
            log.warn("解析流式 chunk 失败，跳过该块: {}", chunk, e);
        }
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

    // ========== AI 总结 ==========

    /**
     * 总结文章内容
     */
    public String summarizeContent(String title, String content) {
        String systemPrompt = "你是一位专业的文章内容总结专家。请阅读以下文章，并输出一段 150-250 字左右的精炼总结，提炼出文章的核心观点和主要内容。要求语言通顺，表达清晰，直接输出总结内容，不要包含任何旁白、前言或后续多余的话。";

        String cleanContent = content != null ? content.replaceAll("<[^>]*>", "").trim() : "";
        if (cleanContent.length() > aiConfig.getMaxContentLength()) {
            cleanContent = cleanContent.substring(0, aiConfig.getMaxContentLength());
        }

        String userMessage = "文章标题：" + title + "\n\n文章正文：\n" + cleanContent;
        Map<String, Object> requestBody = chatRequestBody(systemPrompt, userMessage, 0.5, 1000);
        return callChat("summarize", requestBody).content();
    }

    // ========== 内部统一调用入口 ==========

    /**
     * 所有非流式 LLM 调用的统一入口：并发闸门 + 截断检查 + 指标埋点（调用量/耗时/token）。
     */
    private ChatOutcome callChat(String endpointTag, Map<String, Object> requestBody) {
        long start = System.currentTimeMillis();
        String result = "success";
        boolean acquired = false;
        try {
            try {
                acquired = llmCallSlots.tryAcquire(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("AI 调用被中断");
            }
            if (!acquired) {
                result = "busy";
                throw new RuntimeException("当前 AI 使用人数较多，请稍后重试");
            }
            Map<String, Object> response = executeChat(requestBody);
            List<Map<String, Object>> choices = castList(response.get("choices"));
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("AI 响应缺少 choices");
            }
            Map<String, Object> choice = choices.get(0);
            if ("length".equals(choice.get("finish_reason"))) {
                result = "truncated";
                throw new AiOutputTruncatedException();
            }
            Map<String, Object> message = castMap(choice.get("message"));
            String content = message == null ? null : (String) message.get("content");
            if (content == null) {
                throw new RuntimeException("AI 响应缺少 content");
            }
            Map<String, Object> usage = castMap(response.get("usage"));
            Integer promptTokens = usage == null ? null : intOf(usage.get("prompt_tokens"));
            Integer completionTokens = usage == null ? null : intOf(usage.get("completion_tokens"));
            aiMetrics.recordTokens(endpointTag, promptTokens, completionTokens);
            return new ChatOutcome(content, promptTokens, completionTokens);
        } catch (AiOutputTruncatedException e) {
            throw e;
        } catch (Exception e) {
            result = "error".equals(result) ? result : result;
            log.error("AI 调用失败（{}）", endpointTag, e);
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "AI 调用失败");
        } finally {
            if (acquired) {
                llmCallSlots.release();
            }
            aiMetrics.recordCall(endpointTag, result, System.currentTimeMillis() - start);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeChat(Map<String, Object> requestBody) {
        return aiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .block();
    }

    private Map<String, Object> chatRequestBody(String systemPrompt, String userMessage,
                                                double temperature, int maxTokens) {
        return Map.of(
                "model", aiConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", temperature,
                "max_tokens", maxTokens
        );
    }

    private Map<String, Object> simpleRequestBody(String userPrompt, double temperature, int maxTokens) {
        return Map.of(
                "model", aiConfig.getModel(),
                "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                "temperature", temperature,
                "max_tokens", maxTokens
        );
    }

    /** 通用内容生成（历史兼容入口） */
    public String generateContent(String userPrompt, double temperature, int maxTokens) {
        return callChat("content", simpleRequestBody(userPrompt, temperature, maxTokens)).content();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object obj) {
        return (List<Map<String, Object>>) obj;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    private Integer intOf(Object obj) {
        return obj instanceof Number n ? n.intValue() : null;
    }

    // ========== 结果缓存 ==========

    private String cachedString(String op, String content, String title, Supplier<String> loader) {
        String key = CACHE_KEY_PREFIX + op + ":" + contentHash(content, title);
        return cacheService.getOrLoad(key, CACHE_TTL, () -> Optional.of(loader.get()), String.class)
                .orElseThrow(() -> new RuntimeException("AI 调用失败：返回内容为空"));
    }

    private String contentHash(String content, String title) {
        String raw = (content == null ? "" : content) + "|"
                + (title == null ? "" : title) + "|"
                + aiConfig.getModel();
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    // ========== 向量 Embedding ==========

    /**
     * 生成文本 Embedding。
     * <p>
     * 优先调用配置的真实 Embedding 服务（OpenAI 兼容 /embeddings，默认 SiliconFlow
     * 的 BAAI/bge-m3）；api-key 未配置（开发模式）或调用失败时降级为 LLM 伪向量，
     * 伪向量维度与配置维度一致以保证入库兼容。
     * 注意：真实/伪向量混在同一向量表中相似度不可比——生产必须配置真实服务，
     * 并在切换模型后通过管理端 /api/vectors/admin/rebuild 全量重建。
     */
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String cleanText = text.replaceAll("<[^>]*>", "").trim();
        if (cleanText.length() > aiConfig.getMaxContentLength()) {
            cleanText = cleanText.substring(0, aiConfig.getMaxContentLength());
        }

        AiConfig.EmbeddingSetting embedding = aiConfig.getEmbedding();
        if (embedding.getApiKey() == null || embedding.getApiKey().isBlank()) {
            log.warn("ai.embedding.api-key 未配置，使用 LLM 伪向量（仅限开发环境，检索质量不可靠）");
            return generateEmbeddingByLlm(cleanText);
        }
        try {
            return callEmbeddingApi(cleanText);
        } catch (Exception e) {
            log.warn("Embedding 服务调用失败，降级为大模型生成向量: {}", e.getMessage());
        }
        return generateEmbeddingByLlm(cleanText);
    }

    /** 调用 OpenAI 兼容的真实 Embedding 服务，并校验返回维度与配置一致 */
    @SuppressWarnings("unchecked")
    private List<Double> callEmbeddingApi(String text) {
        AiConfig.EmbeddingSetting embedding = aiConfig.getEmbedding();
        Map<String, Object> requestBody = Map.of(
                "model", embedding.getModel(),
                "input", text,
                "encoding_format", "float"
        );

        Map<String, Object> response = webClientBuilder
                .baseUrl(embedding.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + embedding.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .block();

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new RuntimeException("Embedding 响应缺少 data");
        }
        List<Number> vector = (List<Number>) data.get(0).get("embedding");
        if (vector == null || vector.isEmpty()) {
            throw new RuntimeException("Embedding 响应缺少向量");
        }
        if (vector.size() != embedding.getDimension()) {
            throw new RuntimeException(String.format(
                    "Embedding 维度与配置不一致（期望 %d，实际 %d），请核对 ai.embedding.model 与 dimension",
                    embedding.getDimension(), vector.size()));
        }
        return vector.stream()
                .map(Number::doubleValue)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 无真实 embedding 服务时的开发兜底：确定性哈希向量（字符 bigram → 维度桶累加 → L2 归一化）。
     * 相比旧实现（chat 模型输出 1024 维 JSON 数组——必然超 max_tokens 被截断、且耗 LLM 调用），
     * 哈希向量零成本、稳定可测；检索质量仅反映字面重叠，仅限开发环境。
     */
    private List<Double> generateEmbeddingByLlm(String text) {
        int dimension = aiConfig.getEmbedding().getDimension();
        double[] vector = new double[dimension];
        String cleaned = text.replaceAll("\s+", "");
        for (int i = 0; i < cleaned.length(); i++) {
            String gram = cleaned.substring(i, Math.min(i + 2, cleaned.length()));
            vector[Math.floorMod(gram.hashCode(), dimension)] += 1.0;
        }
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        java.util.List<Double> result = new java.util.ArrayList<>(dimension);
        for (double v : vector) {
            result.add(norm > 0 ? v / norm : 0.0);
        }
        return result;
    }

    private List<Double> generateEmbeddingByLlmLegacy(String text) {
        int dimension = aiConfig.getEmbedding().getDimension();
        String systemPrompt = "你是一个文本向量生成器。请为下面用户提供的文本生成一个 " + dimension + " 维的语义向量。"
                + "向量应能反映文本的主题、关键词和语义信息。"
                + "请只输出一个 JSON 数组，数组长度为 " + dimension + "，元素为 -1 到 1 之间的浮点数，不要输出任何解释。";
        String userPrompt = "文本内容：\n" + text;

        ChatOutcome outcome = callChat("embedding", chatRequestBody(systemPrompt, userPrompt, 0.1, 4096));
        return parseEmbeddingJson(outcome.content().trim());
    }

    private List<Double> parseEmbeddingJson(String raw) {
        String json = raw;
        if (json.startsWith("```")) {
            json = json.replaceAll("```[a-z]*\n", "").replaceAll("\n```", "").trim();
        }
        try {
            List<Double> vector = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Double>>() {
            });
            if (vector.size() != aiConfig.getEmbedding().getDimension()) {
                log.warn("Embedding 维度不匹配，期望 {}，实际 {}",
                        aiConfig.getEmbedding().getDimension(), vector.size());
            }
            return vector;
        } catch (Exception e) {
            log.error("解析 Embedding 向量失败: {}", raw, e);
            throw new RuntimeException("解析 Embedding 向量失败");
        }
    }

    // ========== RAG ==========

    /**
     * 基于检索上下文生成 RAG 回答（兼容旧版调用）
     */
    public String ragQuery(String question, String context) {
        return generateRagAnswer(question, context);
    }

    /**
     * 基于检索上下文生成 RAG 回答，回答中需标注引用来源
     */
    public String generateRagAnswer(String question, String context) {
        String systemPrompt = "你是一个基于文章库的问答助手。请严格根据下面提供的检索到的文章内容回答用户问题，并在回答中标注引用来源。\n\n"
                + "检索到的文章内容：\n" + context + "\n\n"
                + "回答要求：\n"
                + "1. 基于上述文章内容准确回答问题\n"
                + "2. 如果文章中没有相关信息，请诚实告知用户\n"
                + "3. 使用 [1]、[2] 等编号标注引用来源，对应文章编号\n"
                + "4. 回答控制在 500 字以内";

        return callChat("rag", chatRequestBody(systemPrompt, question, 0.5, 1500)).content();
    }

    // ========== V2.0 AI 写作助手 ==========

    /**
     * 一键生成：一次调用同时产出标题候选、摘要与标签。
     * JSON 解析失败或产出为空时，降级为三个独立调用，保证功能可用。
     */
    public AiAssistResponseDto generateAssist(String content, String title) {
        return cacheService.getOrLoad(
                        CACHE_KEY_PREFIX + "assist:" + contentHash(content, title), CACHE_TTL,
                        () -> Optional.ofNullable(doGenerateAssist(content, title)),
                        AiAssistResponseDto.class)
                .orElseThrow(() -> new RuntimeException("AI 一键生成失败，请稍后重试"));
    }

    private AiAssistResponseDto doGenerateAssist(String content, String title) {
        String titleContext = (title != null && !title.isBlank()) ? "当前标题（供参考）：" + title + "\n\n" : "";
        String prompt = "你是中文写作助手。请根据以下文章内容生成写作辅助信息，"
                + "只输出一个 JSON 对象，不要输出任何解释。JSON 包含三个键：\n"
                + "{\"titles\": [3-5个吸引人的标题字符串], \"summary\": \"80-150字的摘要\", \"tags\": [3-8个用于内容分类的标签字符串]}\n\n"
                + titleContext + truncateContent(content);

        Map<String, Object> requestBody = new java.util.HashMap<>(
                simpleRequestBody(prompt, 0.5, 1200));
        requestBody.put("response_format", Map.of("type", "json_object"));

        try {
            String result = callChat("assist", requestBody).content();
            AiAssistResponseDto dto = fromAssistJson(result);
            if (!dto.getTitles().isEmpty() || isNotBlank(dto.getSummary()) || !dto.getTags().isEmpty()) {
                return dto;
            }
            log.warn("AI 一键生成 JSON 内容为空，降级为独立调用");
        } catch (AiOutputTruncatedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 一键生成失败，降级为独立调用: {}", e.getMessage());
        }

        AiAssistResponseDto dto = new AiAssistResponseDto();
        dto.setTitles(generateTitles(content, title));
        dto.setSummary(generateSummary(content, title));
        dto.setTags(generateTags(content, title));
        return dto;
    }

    private AiAssistResponseDto fromAssistJson(String raw) {
        Map<String, Object> parsed = parseJsonSafely(raw);
        AiAssistResponseDto dto = new AiAssistResponseDto();
        dto.setTitles(toStringList(parsed.get("titles")));
        dto.setTags(toStringList(parsed.get("tags")));
        Object summary = parsed.get("summary");
        dto.setSummary(summary instanceof String s ? s.trim() : "");
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object obj) {
        if (!(obj instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(8)
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** 自动生成单个标题 */
    public String generateTitle(String content) {
        return generateTitle(content, null);
    }

    public String generateTitle(String content, String title) {
        List<String> titles = generateTitles(content, title);
        return titles.isEmpty() ? "" : titles.get(0);
    }

    /** 自动生成标题（相同内容 1 小时内命中缓存，不重复计费） */
    public List<String> generateTitles(String content) {
        return generateTitles(content, null);
    }

    public List<String> generateTitles(String content, String title) {
        String raw = cachedString("titles", content, title, () -> {
            String prompt = "根据以下文章内容，生成 3-5 个吸引人的标题候选。"
                    + "每个标题一行，不要编号，不要添加任何解释。\n\n"
                    + titleContext(title) + truncateContent(content);
            return callChat("titles", simpleRequestBody(prompt, 0.8, 500)).content();
        });
        return List.of(raw.split("\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    /** 自动生成摘要 */
    public String generateSummary(String content) {
        return generateSummary(content, null);
    }

    public String generateSummary(String content, String title) {
        return cachedString("summary", content, title, () -> {
            String prompt = "请为以下文章内容生成一段 80-150 字的摘要，提炼核心观点。"
                    + "直接输出摘要内容，不要添加任何解释。\n\n"
                    + titleContext(title) + truncateContent(content);
            return callChat("summary", simpleRequestBody(prompt, 0.5, 300)).content();
        });
    }

    /** 自动生成标签 */
    public List<String> generateTags(String content) {
        return generateTags(content, null);
    }

    public List<String> generateTags(String content, String title) {
        String raw = cachedString("tags", content, title, () -> {
            String prompt = "请从以下文章中提取 3-8 个关键词/标签，用于内容分类。"
                    + "每行一个标签，只输出标签本身，不要编号或解释。\n\n"
                    + titleContext(title) + truncateContent(content);
            return callChat("tags", simpleRequestBody(prompt, 0.5, 200)).content();
        });
        return List.of(raw.split("\n")).stream()
                .map(s -> s.replaceAll("^[\\d\\.\\-、]+\\s*", "").trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    /** 文章结构优化建议 */
    public String optimizeStructure(String content) {
        return cachedString("structure", content, null, () -> {
            String prompt = "你是一位专业编辑。请分析以下文章的结构，给出具体的改进建议："
                    + "1.段落划分是否合理 2.标题层级是否恰当 3.逻辑顺序是否顺畅。"
                    + "请直接输出优化建议，每条一行。\n\n" + truncateContent(content);
            return callChat("structure", simpleRequestBody(prompt, 0.4, 1000)).content();
        });
    }

    /** 自动纠错 */
    public String autoCorrect(String content) {
        return cachedString("correct", content, null, () -> {
            String prompt = "请检查以下文章中的错别字、语法错误、标点问题，"
                    + "并指出具体错误位置和建议修改。格式：'第X处：原文'xxx' → 建议改为'yyy'，原因：...'。"
                    + "如果没有错误，回复'未发现错误'。\n\n" + truncateContent(content);
            return callChat("correct", simpleRequestBody(prompt, 0.3, 1500)).content();
        });
    }

    // ========== V2.0 AI 评论助手 ==========

    /** 生成评论回复 */
    public String generateCommentReply(String articleTitle, String parentComment, String mode) {
        String stylePrompt = switch (mode != null ? mode : "smart") {
            case "professional" -> "请生成一个专业、技术型回复";
            case "humorous" -> "请生成一个轻松幽默的回复";
            case "concise" -> "请生成一个简短精炼的回复（不超过50字）";
            default -> "请生成一个合适的回复";
        };

        String prompt = stylePrompt + "。\n\n"
                + "文章标题：" + articleTitle + "\n"
                + "被回复的评论：" + parentComment + "\n\n"
                + "直接输出回复内容，不要添加任何解释。";
        return callChat("comment-reply", simpleRequestBody(prompt, 0.7, 300)).content();
    }

    // ========== V2.0 增强总结 ==========

    /** 提取关键观点 */
    public List<String> extractKeyPoints(String content) {
        String result = callChat("key-points", simpleRequestBody(
                "请从以下文章中提取 3-5 个关键论点和结论。"
                        + "每条一行，使用'【观点X】内容'的格式。\n\n" + truncateContent(content),
                0.5, 800)).content();
        return List.of(result.split("\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    /** 生成思维导图（Markdown 格式） */
    public String generateMindMap(String content, String title) {
        String prompt = "请根据以下文章内容，生成一个 Markdown 格式的思维导图。"
                + "使用多级列表（- 一级，  - 二级，    - 三级）表示层级关系。"
                + "根节点为文章标题，子节点为核心章节和关键概念。"
                + "只输出 Markdown 列表结构，不要其他内容。\n\n"
                + "文章标题：" + title + "\n\n" + truncateContent(content);
        return "## " + title + "\n" + callChat("mind-map", simpleRequestBody(prompt, 0.6, 2000)).content();
    }

    // ========== V2.0 知识图谱 ==========

    /** 从文章提取概念/技术栈/人物 */
    public Map<String, Object> extractKnowledge(String content) {
        String prompt = "请从以下文章中提取知识实体，返回 JSON 格式：\n"
                + "{\n"
                + "  \"concepts\": [\"概念1\", \"概念2\"],\n"
                + "  \"persons\": [\"人物1\"],\n"
                + "  \"techStack\": [\"技术1\", \"技术2\"],\n"
                + "  \"relations\": [{\"source\": \"概念A\", \"target\": \"概念B\", \"relation\": \"关联描述\"}]\n"
                + "}\n"
                + "只输出 JSON，不要解释。\n\n" + truncateContent(content, 6000);

        String result = callChat("knowledge", simpleRequestBody(prompt, 0.3, 2000)).content();
        try {
            return parseJsonSafely(result);
        } catch (Exception e) {
            log.error("解析知识图谱 JSON 失败: {}", result, e);
            return Map.of("concepts", List.of(), "persons", List.of(), "techStack", List.of(), "relations", List.of());
        }
    }

    // ========== 内部工具方法 ==========

    private String titleContext(String title) {
        return (title != null && !title.isBlank()) ? "参考标题：" + title + "\n\n" : "";
    }

    /** 截断过长内容，保留首部 */
    private String truncateContent(String content) {
        return truncateContent(content, aiConfig.getMaxContentLength());
    }

    private String truncateContent(String content, int maxLen) {
        if (content == null) return "";
        String cleaned = content.replaceAll("<[^>]*>", "").trim();
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen) + "\n...(内容过长已截断)";
        }
        return cleaned;
    }

    /** 安全解析 AI 返回的 JSON */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonSafely(String raw) {
        String json = raw.trim();
        // 去除 markdown 代码块标记
        if (json.startsWith("```")) {
            json = json.replaceAll("```[a-z]*\n", "").replaceAll("\n```", "");
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("JSON 解析失败", e);
            return Map.of();
        }
    }
}
