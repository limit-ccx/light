package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "featured_content", indexes = {
        @Index(name = "idx_featured_position", columnList = "position"),
        @Index(name = "idx_featured_target", columnList = "target_type, target_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 50)
    private String position = "HOME";

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "curated_by", nullable = false)
    private Long curatedBy;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
