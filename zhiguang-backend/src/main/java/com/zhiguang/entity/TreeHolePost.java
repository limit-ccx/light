package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tree_hole_posts", indexes = {
        @Index(name = "idx_thp_status_created", columnList = "status, created_at"),
        @Index(name = "idx_thp_emotion", columnList = "emotion_tag"),
        @Index(name = "idx_thp_score", columnList = "score DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeHolePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anonymous_hash", length = 64, nullable = false)
    private String anonymousHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "emotion_tag", length = 30, nullable = false)
    private String emotionTag;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "empathy_count")
    private Integer empathyCount = 0;

    @Column(name = "reply_count")
    private Integer replyCount = 0;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "report_count")
    private Integer reportCount = 0;

    /**
     * 0: pending 审核中
     * 1: active  已发布
     * 2: rejected 已拒绝
     * 3: deleted 已删除
     */
    @Column(nullable = false)
    private Integer status = 1;

    /**
     * 热度分数，用于推荐排序
     */
    @Column(precision = 10)
    private Double score = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
