package com.zhiguang.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Elasticsearch 文章文档
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDocument {

    private Long id;              // 文章ID
    private String title;         // 标题
    private String content;       // 内容
    private String summary;       // 摘要
    private String authorName;    // 作者名称
    private Long authorId;        // 作者ID
    private String status;        // 状态：PUBLISHED, DRAFT
    private String[] tags;        // 标签数组
    private Integer viewCount;    // 浏览量
    private Integer likeCount;    // 点赞数
    private Integer commentCount; // 评论数
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间

    /**
     * 转换为索引名称
     */
    public static String getIndexName() {
        return "zhiguang_articles";
    }
}