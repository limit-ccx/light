package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.ArticleResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.dto.RagSearchAnswerDto;
import com.zhiguang.service.AiSearchService;
import com.zhiguang.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final AiSearchService searchService;
    private final AiService aiService;
    private final ThreadPoolTaskExecutor aiWorkerExecutor;

    public SearchController(AiSearchService searchService, AiService aiService,
                            @Qualifier("aiWorkerExecutor") ThreadPoolTaskExecutor aiWorkerExecutor) {
        this.searchService = searchService;
        this.aiService = aiService;
        this.aiWorkerExecutor = aiWorkerExecutor;
    }

    /** 智能搜索（基于 MySQL 的语义搜索） */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(searchService.semanticSearch(q, page, size)));
    }

    /** 向量相似度搜索 */
    @GetMapping("/vector")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> vectorSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(searchService.vectorSearch(q, page, size)));
    }

    /**
     * RAG 问答：基于向量检索结果生成回答。
     * LLM 生成耗时 10-60s，以 DeferredResult 释放 Tomcat 线程（与 /api/ai/* 同一 worker 池）。
     */
    @PostMapping("/rag")
    public DeferredResult<ResponseEntity<ApiResponse<RagSearchAnswerDto>>> rag(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "").trim();
        if (question.isBlank()) {
            DeferredResult<ResponseEntity<ApiResponse<RagSearchAnswerDto>>> immediate = new DeferredResult<>();
            immediate.setResult(ResponseEntity.ok(ApiResponse.error("问题不能为空")));
            return immediate;
        }

        DeferredResult<ResponseEntity<ApiResponse<RagSearchAnswerDto>>> deferred = new DeferredResult<>(120_000L);
        deferred.onTimeout(() -> deferred.setErrorResult(
                ResponseEntity.status(504).body(ApiResponse.<RagSearchAnswerDto>error(504, "AI 响应超时，请稍后重试"))));
        try {
            aiWorkerExecutor.execute(() -> {
                try {
                    PageResponse<ArticleResponseDto> relevant = searchService.vectorSearch(question, 0, 5);
                    String context = buildRagContext(relevant.getList());
                    String answer = aiService.generateRagAnswer(question, context);
                    deferred.setResult(ResponseEntity.ok(
                            ApiResponse.success(new RagSearchAnswerDto(answer, relevant.getList()))));
                } catch (Exception e) {
                    log.error("RAG 问答异步执行异常", e);
                    deferred.setErrorResult(ResponseEntity.internalServerError()
                            .body(ApiResponse.<RagSearchAnswerDto>error(500, "AI 服务暂时不可用，请稍后重试")));
                }
            });
        } catch (RejectedExecutionException e) {
            deferred.setErrorResult(ResponseEntity.status(503)
                    .body(ApiResponse.<RagSearchAnswerDto>error(503, "当前使用人数较多，请稍后重试")));
        }
        return deferred;
    }

    private String buildRagContext(List<ArticleResponseDto> articles) {
        if (articles == null || articles.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < articles.size(); i++) {
            ArticleResponseDto article = articles.get(i);
            context.append("【文章").append(i + 1).append("】\n");
            context.append("标题：").append(article.getTitle()).append("\n");
            if (article.getSummary() != null && !article.getSummary().isBlank()) {
                context.append("摘要：").append(article.getSummary()).append("\n");
            }
            if (article.getContent() != null && !article.getContent().isBlank()) {
                String content = article.getContent().replaceAll("<[^>]*>", "").trim();
                if (content.length() > 800) {
                    content = content.substring(0, 800) + "...";
                }
                context.append("内容：").append(content).append("\n");
            }
            context.append("\n");
        }
        return context.toString();
    }

    /** 相关文章推荐 */
    @GetMapping("/related/{articleId}")
    public ResponseEntity<ApiResponse<List<ArticleResponseDto>>> related(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(searchService.getRelatedArticles(articleId, limit)));
    }
}
