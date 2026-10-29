package com.zhiguang.service;

import com.zhiguang.dto.ArticleResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.Article;
import com.zhiguang.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchService {

    private final ArticleRepository articleRepository;
    private final ArticleService articleService;
    private final ArticleEmbeddingService articleEmbeddingService;

    /**
     * 语义搜索：基于关键词与全文匹配的增强搜索
     * 先用 MySQL LIKE 搜索，再对结果按相关度排序
     */
    public PageResponse<ArticleResponseDto> semanticSearch(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "viewCount"));
        Page<Article> articlePage = articleRepository.searchByKeyword(query, pageable);

        List<Long> ids = articlePage.getContent().stream()
                .map(Article::getId)
                .collect(Collectors.toList());
        List<ArticleResponseDto> list = articleService.getPublishedArticlesByIds(ids);

        return new PageResponse<>(list, articlePage.getTotalElements(), page, size, articlePage.getTotalPages());
    }

    /**
     * 向量相似度搜索：基于文章 Embedding（SCAN 渐进扫描 + 批量取回）
     */
    public PageResponse<ArticleResponseDto> vectorSearch(String query, int page, int size) {
        ArticleEmbeddingService.VectorSearchResult result = articleEmbeddingService.vectorSearch(query, page, size);
        if (result.articleIds().isEmpty()) {
            return new PageResponse<>(List.of(), result.total(), page, size, result.totalPages());
        }

        List<ArticleResponseDto> list = articleService.getPublishedArticlesByIds(result.articleIds());

        return new PageResponse<>(list, result.total(), page, size, result.totalPages());
    }

    /**
     * 相关文章推荐：基于标签匹配
     */
    public List<ArticleResponseDto> getRelatedArticles(Long articleId, int limit) {
        Article article = articleRepository.findById(articleId).orElse(null);
        if (article == null || article.getTags() == null || article.getTags().isEmpty()) {
            return List.of();
        }
        // 找标签有交集的文章，按热度排序
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"));
        Page<Article> related = articleRepository.findByTagsIn(article.getTags(), pageable);
        List<Long> ids = related.getContent().stream()
                .filter(a -> !a.getId().equals(articleId))
                .limit(limit)
                .map(Article::getId)
                .collect(Collectors.toList());
        return articleService.getPublishedArticlesByIds(ids);
    }
}
