package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "collection_articles")
@IdClass(CollectionArticle.CollectionArticleId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionArticle {

    @Id
    @Column(name = "collection_id")
    private Long collectionId;

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", insertable = false, updatable = false)
    private ArticleCollection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionArticleId implements Serializable {
        private Long collectionId;
        private Long articleId;
    }
}
