package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "column_articles")
@IdClass(ColumnArticle.ColumnArticleId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnArticle {

    @Id
    @Column(name = "column_id")
    private Long columnId;

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", insertable = false, updatable = false)
    private ColumnEntity column;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnArticleId implements Serializable {
        private Long columnId;
        private Long articleId;
    }
}
