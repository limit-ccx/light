package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tree_hole_reports", indexes = {
        @Index(name = "idx_threport_target", columnList = "target_type, target_id"),
        @Index(name = "idx_threport_unique", columnList = "anonymous_hash, target_type, target_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeHoleReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anonymous_hash", length = 64, nullable = false)
    private String anonymousHash;

    @Column(name = "target_type", length = 10, nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(length = 500)
    private String reason;

    /**
     * 0: pending, 1: resolved, 2: ignored
     */
    @Column(nullable = false)
    private Integer status = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
