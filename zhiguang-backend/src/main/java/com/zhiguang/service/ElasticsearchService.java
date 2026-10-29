package com.zhiguang.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.zhiguang.config.ElasticsearchProperties;
import com.zhiguang.document.ArticleDocument;
import com.zhiguang.entity.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch 搜索服务
 * <p>
 * 提供文章索引管理和搜索功能：
 * - 创建索引
 * - 索引文章
 * - 搜索文章（全文 + 语义）
 * - 删除索引
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchProperties elasticsearchProperties;

    /**
     * 创建文章索引
     */
    public void createArticleIndex() {
        try {
            String indexName = elasticsearchProperties.getIndex().getArticle();
            
            elasticsearchClient.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("id", p -> p.long_(l -> l))
                            .properties("title", p -> p.text(t -> t.analyzer("ik_max_word")))
                            .properties("content", p -> p.text(t -> t.analyzer("ik_max_word")))
                            .properties("summary", p -> p.text(t -> t.analyzer("ik_max_word")))
                            .properties("authorName", p -> p.text(t -> t))
                            .properties("authorId", p -> p.long_(l -> l))
                            .properties("status", p -> p.keyword(k -> k))
                            .properties("tags", p -> p.keyword(k -> k))
                            .properties("viewCount", p -> p.integer(i -> i))
                            .properties("likeCount", p -> p.integer(i -> i))
                            .properties("commentCount", p -> p.integer(i -> i))
                            .properties("createdAt", p -> p.date(d -> d))
                            .properties("updatedAt", p -> p.date(d -> d))
                    )
            );

            log.info("Elasticsearch 文章索引创建成功: index={}", indexName);
        } catch (Exception e) {
            log.error("Elasticsearch 文章索引创建失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 索引文章（创建或更新）
     */
    public void indexArticle(Article article) {
        try {
            ArticleDocument document = convertToDocument(article);
            String indexName = elasticsearchProperties.getIndex().getArticle();

            elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(String.valueOf(article.getId()))
                    .document(document)
            );

            log.debug("文章索引成功: articleId={}, title={}", article.getId(), article.getTitle());
        } catch (Exception e) {
            log.error("文章索引失败: articleId={}, error={}", article.getId(), e.getMessage(), e);
        }
    }

    /**
     * 批量索引文章
     */
    public void bulkIndexArticles(List<Article> articles) {
        try {
            String indexName = elasticsearchProperties.getIndex().getArticle();

            elasticsearchClient.bulk(b -> {
                for (Article article : articles) {
                    ArticleDocument document = convertToDocument(article);
                    b.operations(o -> o
                            .index(idx -> idx
                                    .index(indexName)
                                    .id(String.valueOf(article.getId()))
                                    .document(document)
                            )
                    );
                }
                return b;
            });

            log.info("批量索引文章成功: count={}", articles.size());
        } catch (Exception e) {
            log.error("批量索引文章失败: count={}, error={}", articles.size(), e.getMessage(), e);
        }
    }

    /**
     * 搜索文章（全文检索）
     */
    public List<ArticleDocument> searchArticles(String keyword, int page, int size) {
        try {
            String indexName = elasticsearchProperties.getIndex().getArticle();

            SearchResponse<ArticleDocument> response = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .multiMatch(mm -> mm
                                                    .fields("title", "content", "summary", "authorName")
                                                    .query(keyword)
                                                    .analyzer("ik_max_word")
                                            )
                                    )
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("status")
                                                    .value("PUBLISHED")
                                            )
                                    )
                            )
                    )
                    .from(page * size)
                    .size(size)
                    .sort(so -> so
                            .field(f -> f
                                    .field("createdAt")
                                    .order(SortOrder.Desc)
                            )
                    )
            , ArticleDocument.class);

            List<ArticleDocument> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.debug("搜索文章成功: keyword={}, count={}", keyword, results.size());
            return results;
        } catch (Exception e) {
            log.error("搜索文章失败: keyword={}, error={}", keyword, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索文章（带标签过滤）
     */
    public List<ArticleDocument> searchArticlesByTag(String keyword, String[] tags, int page, int size) {
        try {
            String indexName = elasticsearchProperties.getIndex().getArticle();

            SearchResponse<ArticleDocument> response = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .query(q -> q
                            .bool(b -> {
                                // 关键词搜索
                                b.must(m -> m
                                        .multiMatch(mm -> mm
                                                .fields("title", "content", "summary")
                                                .query(keyword)
                                                .analyzer("ik_max_word")
                                        )
                                );

                                // 标签过滤
                                if (tags != null && tags.length > 0) {
                                    b.filter(f -> f
                                            .terms(t -> t
                                            .field("tags")
                                            .terms(ts -> ts.value(Arrays.stream(tags)
                                                    .map(FieldValue::of)
                                                    .toList()))
                                    )
                                    );
                                }

                                // 状态过滤
                                b.filter(f -> f
                                        .term(t -> t
                                                .field("status")
                                                .value("PUBLISHED")
                                        )
                                );

                                return b;
                            })
                    )
                    .from(page * size)
                    .size(size)
                    .sort(so -> so
                            .field(f -> f
                                    .field("createdAt")
                                    .order(SortOrder.Desc)
                            )
                    )
            , ArticleDocument.class);

            List<ArticleDocument> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.debug("搜索文章成功（带标签）: keyword={}, tags={}, count={}", keyword, tags, results.size());
            return results;
        } catch (Exception e) {
            log.error("搜索文章失败（带标签）: keyword={}, tags={}, error={}", keyword, tags, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除文章索引
     */
    public void deleteArticleIndex(Long articleId) {
        try {
            String indexName = elasticsearchProperties.getIndex().getArticle();

            elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(String.valueOf(articleId))
            );

            log.debug("删除文章索引成功: articleId={}", articleId);
        } catch (Exception e) {
            log.error("删除文章索引失败: articleId={}, error={}", articleId, e.getMessage(), e);
        }
    }

    /**
     * 转换 Article 实体到 ArticleDocument
     */
    private ArticleDocument convertToDocument(Article article) {
        return new ArticleDocument(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getSummary(),
                article.getAuthor() != null ? article.getAuthor().getUsername() : "",
                article.getAuthor() != null ? article.getAuthor().getId() : 0L,
                article.getStatus() != null ? String.valueOf(article.getStatus()) : "1",
                article.getTags() != null ? article.getTags().toArray(new String[0]) : new String[0],
                article.getViewCount(),
                article.getLikeCount(),
                article.getCommentCount(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}