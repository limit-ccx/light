package com.zhiguang.service;

import com.zhiguang.entity.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章向量存储（2026-10-24 起整体迁移至 pgvector）。
 * <p>
 * 此前向量逐篇存 Redis 并在检索时 SCAN 全量 key + 请求线程算余弦，
 * 文章量增长后既是性能瓶颈也占共享 Redis；现写入/删除/检索全部走
 * pgvector（IVFFlat ANN 索引），Redis 中遗留的 article:embedding:* 键
 * 不再使用，可用 redis-cli 清理：
 * <pre>redis-cli --scan --pattern 'article:embedding:*' | xargs redis-cli del</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleEmbeddingService {

    private static final int MAX_CONTENT_LEN = 3000;

    private final AiService aiService;
    private final VectorSearchService vectorSearchService;

    /**
     * 异步构建并保存文章向量
     */
    @Async
    public void buildEmbeddingAsync(Article article) {
        try {
            buildEmbedding(article);
        } catch (Exception e) {
            log.error("异步构建文章 Embedding 失败，articleId={}", article.getId(), e);
        }
    }

    /**
     * 构建并保存文章向量到 pgvector
     */
    public void buildEmbedding(Article article) {
        if (article == null || article.getId() == null) {
            return;
        }
        String text = buildEmbeddingText(article);
        if (text.isBlank()) {
            return;
        }

        List<Double> vector = aiService.generateEmbedding(text);
        if (vector == null || vector.isEmpty()) {
            log.warn("文章 Embedding 为空，articleId={}", article.getId());
            return;
        }

        float[] floats = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            floats[i] = vector.get(i).floatValue();
        }

        // 内容摘要列仅存截断文本，供混合检索/展示使用
        String snippet = text.length() > 500 ? text.substring(0, 500) : text;
        vectorSearchService.saveEmbedding(article.getId(), article.getTitle(), snippet, floats);
        log.info("文章 Embedding 已保存（pgvector），articleId={}", article.getId());
    }

    /**
     * 删除文章向量
     */
    public void deleteEmbedding(Long articleId) {
        if (articleId == null) {
            return;
        }
        vectorSearchService.deleteEmbedding(articleId);
        log.info("文章 Embedding 已删除，articleId={}", articleId);
    }

    /**
     * 向量相似度搜索（pgvector ANN，分页）
     */
    public VectorSearchResult vectorSearch(String query, int page, int size) {
        if (query == null || query.isBlank() || size <= 0 || page < 0) {
            return VectorSearchResult.empty();
        }

        List<Double> queryVector;
        try {
            queryVector = aiService.generateEmbedding(query);
        } catch (Exception e) {
            log.error("生成查询向量失败", e);
            return VectorSearchResult.empty();
        }
        if (queryVector == null || queryVector.isEmpty()) {
            return VectorSearchResult.empty();
        }

        float[] floats = new float[queryVector.size()];
        for (int i = 0; i < queryVector.size(); i++) {
            floats[i] = queryVector.get(i).floatValue();
        }

        long total = vectorSearchService.countEmbeddings();
        if (total == 0) {
            return VectorSearchResult.empty();
        }

        List<Long> articleIds = vectorSearchService.searchArticleIds(floats, page * size, size)
                .stream().collect(Collectors.toList());

        return new VectorSearchResult(articleIds, total, page, size, (int) ((total + size - 1) / size));
    }

    private String buildEmbeddingText(Article article) {
        StringBuilder sb = new StringBuilder();
        if (article.getTitle() != null && !article.getTitle().isBlank()) {
            sb.append("标题：").append(article.getTitle()).append("\n");
        }
        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            sb.append("摘要：").append(article.getSummary()).append("\n");
        }
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            sb.append("标签：").append(String.join(",", article.getTags())).append("\n");
        }
        if (article.getContent() != null && !article.getContent().isBlank()) {
            String content = article.getContent().replaceAll("<[^>]*>", "").trim();
            if (content.length() > MAX_CONTENT_LEN) {
                content = content.substring(0, MAX_CONTENT_LEN);
            }
            sb.append("正文：").append(content);
        }
        return sb.toString().trim();
    }

    /**
     * 向量搜索结果
     */
    public record VectorSearchResult(List<Long> articleIds, long total, int page, int size, int totalPages) {
        public static VectorSearchResult empty() {
            return new VectorSearchResult(Collections.emptyList(), 0, 0, 0, 0);
        }
    }
}
