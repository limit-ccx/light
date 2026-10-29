package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tree_hole_replies", indexes = {
        @Index(name = "idx_thr_post", columnList = "post_id"),
        @Index(name = "idx_thr_status", columnList = "status, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeHoleReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "anonymous_hash", length = 64, nullable = false)
    private String anonymousHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "empathy_count")
    private Integer empathyCount = 0;

    /**
     * 1: active, 2: rejected, 3: deleted
     */
    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "is_ai_generated")
    private Boolean isAiGenerated = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
