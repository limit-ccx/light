package com.zhiguang.controller;

import com.zhiguang.config.AiConfig;
import com.zhiguang.dto.*;
import com.zhiguang.exception.AiOutputTruncatedException;
import com.zhiguang.service.AiQuotaService;
import com.zhiguang.service.AiService;
import com.zhiguang.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * AI 端点。
 * <p>
 * LLM 调用耗时 10-60s：同步处理会占住 Tomcat 工作线程，并发场景可耗尽线程池
 * 导致整站不可用（PERF-P0-2）。因此除流式端点（SseEmitter）外，所有同步端点
 * 均以 {@link org.springframework.web.context.request.async.DeferredResult} 返回：
 * 校验与配额在 Servlet 线程完成，LLM 调用提交到 aiWorkerExecutor，
 * 对前端仍是普通 JSON 响应（零改动）。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiConfig aiConfig;
    private final AiQuotaService aiQuotaService;
    private final ArticleService articleService;
    private final ThreadPoolTaskExecutor aiWorkerExecutor;

    // 显式构造器：@Qualifier 必须标注在注入点（Lombok @RequiredArgsConstructor 不会拷贝该注解）
    public AiController(AiService aiService, AiConfig aiConfig, AiQuotaService aiQuotaService,
                        ArticleService articleService,
                        @Qualifier("aiWorkerExecutor") ThreadPoolTaskExecutor aiWorkerExecutor) {
        this.aiService = aiService;
        this.aiConfig = aiConfig;
        this.aiQuotaService = aiQuotaService;
        this.articleService = articleService;
        this.aiWorkerExecutor = aiWorkerExecutor;
    }

    private static final long DEFER_TIMEOUT_MS = 120_000L;

    // ========== 通用异步提交 ==========

    private <T> DeferredResult<ResponseEntity<ApiResponse<T>>> submit(
            String tag, Supplier<ResponseEntity<ApiResponse<T>>> work) {
        DeferredResult<ResponseEntity<ApiResponse<T>>> deferred =
                new DeferredResult<>(DEFER_TIMEOUT_MS);
        deferred.onTimeout(() -> deferred.setErrorResult(
                ResponseEntity.status(504).body(ApiResponse.<T>error(504, "AI 响应超时，请稍后重试"))));
        try {
            aiWorkerExecutor.execute(() -> {
                try {
                    deferred.setResult(work.get());
                } catch (AiOutputTruncatedException e) {
                    deferred.setErrorResult(ResponseEntity.badRequest().body(ApiResponse.<T>error(e.getMessage())));
                } catch (Exception e) {
                    log.error("AI 接口[{}]异步执行异常", tag, e);
                    deferred.setErrorResult(ResponseEntity.internalServerError()
                            .body(ApiResponse.<T>error(500, "AI 服务暂时不可用，请稍后重试")));
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("AI worker 池已满，拒绝请求: {}", tag);
            deferred.setErrorResult(ResponseEntity.status(503)
                    .body(ApiResponse.<T>error(503, "当前 AI 使用人数较多，请稍后重试")));
        }
        return deferred;
    }

    private <T> ResponseEntity<ApiResponse<T>> quotaExceeded() {
        return ResponseEntity.status(429)
                .body(ApiResponse.error(429, "今日 AI 使用次数已用完，请明天再试"));
    }

    private boolean consumeQuota() {
        return aiQuotaService.tryConsume(currentUserId());
    }

    // ========== 润色 ==========

    /** AI 润色文章 */
    @PostMapping("/polish")
    public DeferredResult<ResponseEntity<ApiResponse<PolishResponseDto>>> polish(
            @Valid @RequestBody PolishRequestDto request) {
        if (request.getContent().length() > aiConfig.getMaxContentLength()) {
            return completed(ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容超过最大长度限制（" + aiConfig.getMaxContentLength() + " 字符）")));
        }
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("polish", () ->
                ResponseEntity.ok(ApiResponse.success("润色成功", aiService.polishContent(request))));
    }

    /**
     * AI 润色文章（SSE 流式）。
     * 事件：status → delta(text)* → done{model,length,truncated} / error{code,message}
     */
    @PostMapping(value = "/polish/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter polishStream(@Valid @RequestBody PolishRequestDto request) {
        if (request.getContent().length() > aiConfig.getMaxContentLength()) {
            SseEmitter emitter = new SseEmitter(0L);
            return emitterWithError(emitter, "CONTENT_TOO_LONG",
                    "内容超过最大长度限制（" + aiConfig.getMaxContentLength() + " 字符）");
        }
        if (!consumeQuota()) {
            SseEmitter emitter = new SseEmitter(0L);
            return emitterWithError(emitter, "QUOTA_EXCEEDED", "今日 AI 使用次数已用完，请明天再试");
        }

        SseEmitter emitter = new SseEmitter(90_000L);
        try {
            aiService.streamPolishContent(request, emitter);
        } catch (RejectedExecutionException e) {
            log.warn("AI 流式线程池已满，拒绝本次请求");
            return emitterWithError(emitter, "BUSY", "当前使用人数较多，请稍后重试");
        }
        return emitter;
    }

    /** AI 总结文章（force 重算时计入配额；命中文章缓存不消耗） */
    @PostMapping("/summarize/{articleId}")
    public DeferredResult<ResponseEntity<ApiResponse<String>>> summarize(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "false") boolean force) {
        if (force && !consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("summarize", () ->
                ResponseEntity.ok(ApiResponse.success("总结成功", articleService.summarizeArticle(articleId, force))));
    }

    // ========== V2.0 AI 写作助手 ==========

    /** 一键生成：标题 + 摘要 + 标签 一次返回 */
    @PostMapping("/write/assist")
    public DeferredResult<ResponseEntity<ApiResponse<AiAssistResponseDto>>> assist(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (request.getContent().length() > aiConfig.getMaxContentLength()) {
            return completed(ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容超过最大长度限制（" + aiConfig.getMaxContentLength() + " 字符）")));
        }
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("assist", () -> ResponseEntity.ok(ApiResponse.success(
                aiService.generateAssist(request.getContent(), request.getTitle()))));
    }

    /** 自动生成标题 */
    @PostMapping("/write/titles")
    public DeferredResult<ResponseEntity<ApiResponse<List<String>>>> generateTitles(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("titles", () -> ResponseEntity.ok(ApiResponse.success(
                aiService.generateTitles(request.getContent(), request.getTitle()))));
    }

    /** 自动生成摘要 */
    @PostMapping("/write/summary")
    public DeferredResult<ResponseEntity<ApiResponse<String>>> generateSummary(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("summary", () -> ResponseEntity.ok(ApiResponse.success(
                aiService.generateSummary(request.getContent(), request.getTitle()))));
    }

    /** 自动生成标签 */
    @PostMapping("/write/tags")
    public DeferredResult<ResponseEntity<ApiResponse<List<String>>>> generateTags(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("tags", () -> ResponseEntity.ok(ApiResponse.success(
                aiService.generateTags(request.getContent(), request.getTitle()))));
    }

    /** 文章结构优化建议（复用润色对比弹窗展示，返回结构须与润色接口一致） */
    @PostMapping("/write/structure")
    public DeferredResult<ResponseEntity<ApiResponse<PolishResponseDto>>> optimizeStructure(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("structure", () -> {
            String suggestion = aiService.optimizeStructure(request.getContent());
            return ResponseEntity.ok(ApiResponse.success(new PolishResponseDto(
                    request.getContent(), suggestion, "structure", aiConfig.getModel())));
        });
    }

    /** 自动纠错（复用润色对比弹窗展示，返回结构须与润色接口一致） */
    @PostMapping("/write/correct")
    public DeferredResult<ResponseEntity<ApiResponse<PolishResponseDto>>> autoCorrect(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("correct", () -> {
            String result = aiService.autoCorrect(request.getContent());
            return ResponseEntity.ok(ApiResponse.success(new PolishResponseDto(
                    request.getContent(), result, "correct", aiConfig.getModel())));
        });
    }

    // ========== V2.0 AI 评论助手 ==========

    /** 生成评论回复 */
    @PostMapping("/comment/reply")
    public DeferredResult<ResponseEntity<ApiResponse<String>>> generateCommentReply(
            @RequestBody Map<String, String> request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("comment-reply", () -> {
            String articleTitle = request.getOrDefault("articleTitle", "");
            String parentComment = request.getOrDefault("parentComment", "");
            String mode = request.getOrDefault("mode", "smart");
            return ResponseEntity.ok(ApiResponse.success(
                    aiService.generateCommentReply(articleTitle, parentComment, mode)));
        });
    }

    // ========== V2.0 增强总结 ==========

    /** 提取关键观点 */
    @PostMapping("/write/key-points")
    public DeferredResult<ResponseEntity<ApiResponse<List<String>>>> extractKeyPoints(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("key-points", () ->
                ResponseEntity.ok(ApiResponse.success(aiService.extractKeyPoints(request.getContent()))));
    }

    /** 生成思维导图 */
    @PostMapping("/write/mind-map")
    public DeferredResult<ResponseEntity<ApiResponse<String>>> generateMindMap(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("mind-map", () -> ResponseEntity.ok(ApiResponse.success(
                aiService.generateMindMap(request.getContent(), request.getTitle()))));
    }

    // ========== V2.0 知识图谱 ==========

    /** 提取知识实体 */
    @PostMapping("/knowledge/extract")
    public DeferredResult<ResponseEntity<ApiResponse<Map<String, Object>>>> extractKnowledge(
            @Valid @RequestBody AiWriteRequestDto request) {
        if (!consumeQuota()) {
            return completed(quotaExceeded());
        }
        return submit("knowledge", () ->
                ResponseEntity.ok(ApiResponse.success(aiService.extractKnowledge(request.getContent()))));
    }

    // ========== 工具方法 ==========

    private <T> DeferredResult<ResponseEntity<ApiResponse<T>>> completed(
            ResponseEntity<ApiResponse<T>> value) {
        DeferredResult<ResponseEntity<ApiResponse<T>>> deferred =
                new DeferredResult<>();
        deferred.setResult(value);
        return deferred;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.zhiguang.entity.User user) {
            return user.getId();
        }
        return null;
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
