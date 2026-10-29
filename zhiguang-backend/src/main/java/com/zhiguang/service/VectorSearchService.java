package com.zhiguang.service;

import com.zhiguang.config.VectorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zhiguang.dto.VectorSearchDto;

/**
 * 向量检索服务（基于 pgvector）
 * <p>
 * 提供向量存储和检索功能：
 * - 创建向量表
 * - 存储文章 Embedding
 * - 向量相似度搜索（ANN）
 * - 混合检索（向量 + 全文）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final JdbcTemplate vectorJdbcTemplate;
    private final VectorProperties vectorProperties;

    /**
     * 初始化向量表和索引（启动时执行：此前无任何调用方，pgvector 表从未被创建）
     */
    @jakarta.annotation.PostConstruct
    public void initializeVectorTable() {
        JdbcTemplate jdbcTemplate = vectorJdbcTemplate;
        int dimension = vectorProperties.getPostgres().getDimension();

        try {
            // 启用 pgvector 扩展
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

            // 创建文章向量表
            jdbcTemplate.execute(String.format(
                    "CREATE TABLE IF NOT EXISTS article_embeddings (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "article_id BIGINT UNIQUE NOT NULL, " +
                    "title TEXT, " +
                    "content TEXT, " +
                    "embedding vector(%d), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")",
                    dimension
            ));

            // 创建向量索引（IVFFlat 算法，适合中等规模数据）
            jdbcTemplate.execute(String.format(
                    "CREATE INDEX IF NOT EXISTS article_embedding_idx " +
                    "ON article_embeddings USING ivfflat (embedding vector_cosine_ops) " +
                    "WITH (lists = 100)",
                    dimension
            ));

            // 创建 article_id 索引
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS article_id_idx " +
                    "ON article_embeddings (article_id)"
            );

            log.info("向量表初始化成功: dimension={}", dimension);
        } catch (Exception e) {
            log.error("向量表初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 存储文章 Embedding
     */
    public void saveEmbedding(Long articleId, String title, String content, float[] embedding) {
        JdbcTemplate jdbcTemplate = vectorJdbcTemplate;

        int dimension = vectorProperties.getPostgres().getDimension();
        if (embedding == null || embedding.length != dimension) {
            log.error("Embedding 存储失败（维度不匹配）: articleId={}, 期望={}, 实际={}",
                    articleId, dimension, embedding == null ? 0 : embedding.length);
            return;
        }

        try {
            // 将 float[] 转换为 PostgreSQL vector 格式字符串
            String vectorStr = convertToVectorString(embedding);

            // 使用 UPSERT（INSERT ON CONFLICT UPDATE）
            jdbcTemplate.update(
                    "INSERT INTO article_embeddings (article_id, title, content, embedding) " +
                    "VALUES (?, ?, ?, ?::vector) " +
                    "ON CONFLICT (article_id) DO UPDATE SET " +
                    "title = EXCLUDED.title, " +
                    "content = EXCLUDED.content, " +
                    "embedding = EXCLUDED.embedding, " +
                    "updated_at = CURRENT_TIMESTAMP",
                    articleId, title, content, vectorStr
            );

            log.debug("Embedding 存储成功: articleId={}", articleId);
        } catch (Exception e) {
            log.error("Embedding 存储失败: articleId={}, error={}", articleId, e.getMessage(), e);
        }
    }

    /**
     * 向量相似度搜索
     */
    public List<Map<String, Object>> searchSimilarArticles(float[] queryEmbedding, int limit, double threshold) {
        try {
            String vectorStr = convertToVectorString(queryEmbedding);
            // 参数化绑定（此前用 String.format 拼 SQL 且格式参数错位，查询必然异常）
            return vectorJdbcTemplate.queryForList(
                    "SELECT article_id, title, content, 1 - (embedding <=> ?::vector) AS similarity " +
                    "FROM article_embeddings " +
                    "WHERE 1 - (embedding <=> ?::vector) > ? " +
                    "ORDER BY embedding <=> ?::vector " +
                    "LIMIT ?",
                    vectorStr, vectorStr, threshold, vectorStr, limit
            );
        } catch (Exception e) {
            log.error("向量搜索失败: error={}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 分页检索文章 ID（按相似度排序），供文章向量搜索列表使用。
     */
    public List<Long> searchArticleIds(float[] queryEmbedding, int offset, int limit) {
        try {
            String vectorStr = convertToVectorString(queryEmbedding);
            return vectorJdbcTemplate.queryForList(
                    "SELECT article_id FROM article_embeddings " +
                    "ORDER BY embedding <=> ?::vector " +
                    "LIMIT ? OFFSET ?",
                    Long.class, vectorStr, limit, offset
            );
        } catch (Exception e) {
            log.error("分页向量检索失败: error={}", e.getMessage(), e);
            return List.of();
        }
    }

    /** 向量表中的向量总数 */
    public long countEmbeddings() {
        return count(null);
    }

    /** 删除向量表（管理员重建流程用：换模型/维度后必须重建） */
    public void dropEmbeddingTable() {
        try {
            vectorJdbcTemplate.execute("DROP TABLE IF EXISTS article_embeddings");
            log.warn("向量表已删除（管理员操作），随后将按当前维度重建");
        } catch (Exception e) {
            log.error("删除向量表失败: {}", e.getMessage(), e);
        }
    }

    /** 当前配置的向量维度 */
    public int currentDimension() {
        return vectorProperties.getPostgres().getDimension();
    }

    /**
     * 混合检索（向量 + 全文）
     */
    public List<Map<String, Object>> hybridSearch(String keyword, float[] queryEmbedding, int limit) {
        JdbcTemplate jdbcTemplate = vectorJdbcTemplate;

        try {
            String vectorStr = convertToVectorString(queryEmbedding);

            // 向量相似度 + 全文检索混合
            String sql = String.format(
                    "SELECT article_id, title, content, " +
                    "1 - (embedding <=> '%s'::vector) AS vector_score, " +
                    "ts_rank_cd(text_search_vector, plainto_tsquery('%s')) AS text_score, " +
                    "(1 - (embedding <=> '%s'::vector) + ts_rank_cd(text_search_vector, plainto_tsquery('%s'))) / 2 AS combined_score " +
                    "FROM article_embeddings " +
                    "WHERE text_search_vector @@ plainto_tsquery('%s') " +
                    "ORDER BY combined_score DESC " +
                    "LIMIT %d",
                    vectorStr, keyword, vectorStr, keyword, keyword, limit
            );

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            log.debug("混合搜索成功: keyword={}, limit={}, resultCount={}",
                    keyword, limit, results.size());

            return results;
        } catch (Exception e) {
            log.error("混合搜索失败: keyword={}, error={}", keyword, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 删除文章 Embedding
     */
    public void deleteEmbedding(Long articleId) {
        JdbcTemplate jdbcTemplate = vectorJdbcTemplate;

        try {
            jdbcTemplate.update(
                    "DELETE FROM article_embeddings WHERE article_id = ?",
                    articleId
            );

            log.debug("Embedding 删除成功: articleId={}", articleId);
        } catch (Exception e) {
            log.error("Embedding 删除失败: articleId={}, error={}", articleId, e.getMessage(), e);
        }
    }

    /**
     * 文本相似性搜索（自动生成 embedding）
     */
    public List<VectorSearchDto> searchByText(String type, String query, int topK,
                                                Function<String, List<Double>> embeddingFunction) {
        List<Double> vector = embeddingFunction.apply(query);
        return search(type, vector, topK);
    }

    /**
     * 向量相似性搜索
     */
    public List<VectorSearchDto> search(String type, List<Double> vector, int topK) {
        if (vector == null || vector.isEmpty()) {
            return List.of();
        }

        float[] queryEmbedding = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            queryEmbedding[i] = vector.get(i).floatValue();
        }

        List<Map<String, Object>> results = searchSimilarArticles(queryEmbedding, topK, 0.0);
        return results.stream().map(row -> {
            VectorSearchDto dto = new VectorSearchDto();
            dto.setId(((Number) row.get("article_id")).longValue());
            dto.setEntityId(((Number) row.get("article_id")).longValue());
            dto.setTitle((String) row.get("title"));
            dto.setScore(((Number) row.get("similarity")).doubleValue());
            dto.setType(type != null ? type : "ARTICLE");
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取向量总数
     */
    public long count(String type) {
        JdbcTemplate jdbcTemplate = vectorJdbcTemplate;
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM article_embeddings", Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("向量统计失败: error={}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 删除某类型所有向量
     */
    public void deleteVectorsByType(String type) {
        JdbcTemplate jdbcTemplate = vectorJdbcTemplate;
        try {
            jdbcTemplate.update("DELETE FROM article_embeddings");
            log.info("已删除所有向量数据");
        } catch (Exception e) {
            log.error("删除向量失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 将 float[] 转换为 PostgreSQL vector 格式字符串
     */
    private String convertToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}