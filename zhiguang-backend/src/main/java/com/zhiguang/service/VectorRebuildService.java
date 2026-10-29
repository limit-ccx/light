package com.zhiguang.service;

import com.zhiguang.entity.Article;
import com.zhiguang.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 向量库管理操作（管理员触发）。
 * <p>
 * 更换 embedding 模型或调整维度后，旧向量与新向量不在同一语义空间，
 * 相似度计算无意义，必须重建向量表并全量重新嵌入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorRebuildService {

    private static final int PAGE_SIZE = 100;

    private final ArticleRepository articleRepository;
    private final ArticleEmbeddingService articleEmbeddingService;
    private final VectorSearchService vectorSearchService;

    /**
     * 重建向量表：DROP 后按当前配置维度重建（含 IVFFlat 索引）。
     * 同步执行，返回重建后的表维度。
     */
    public int recreateTable() {
        vectorSearchService.dropEmbeddingTable();
        vectorSearchService.initializeVectorTable();
        return vectorSearchService.currentDimension();
    }

    /**
     * 全量重嵌入：分页遍历已发布文章，逐篇生成向量写回 pgvector。
     * 异步执行（每篇一次 LLM/embedding 调用，耗时随文章量增长），
     * 进度可通过 GET /api/vectors/count 观察。
     */
    @Async
    public void rebuildAllAsync() {
        long total = articleRepository.count();
        long rebuilt = 0;
        long failed = 0;
        int page = 0;
        log.info("全量向量重建开始：文章总数 {}", total);

        Page<Article> batch;
        do {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);
            batch = articleRepository.findAll(pageable);
            for (Article article : batch.getContent()) {
                if (article.getStatus() == null || article.getStatus() != 2) {
                    continue;
                }
                try {
                    articleEmbeddingService.buildEmbedding(article);
                    rebuilt++;
                } catch (Exception e) {
                    failed++;
                    log.error("重建向量失败: articleId={}", article.getId(), e);
                }
            }
            page++;
            if (page % 10 == 0) {
                log.info("全量向量重建进度: {}/{}（失败 {}）", Math.min(page * PAGE_SIZE, total), total, failed);
            }
        } while (batch.hasNext());

        log.info("全量向量重建完成：成功 {}，失败 {}", rebuilt, failed);
    }
}
