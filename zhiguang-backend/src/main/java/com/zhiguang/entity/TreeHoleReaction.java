package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tree_hole_reactions", indexes = {
        @Index(name = "idx_threact_target", columnList = "target_type, target_id"),
        @Index(name = "idx_threact_unique", columnList = "anonymous_hash, target_type, target_id, reaction_type", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeHoleReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anonymous_hash", length = 64, nullable = false)
    private String anonymousHash;

    /**
     * POST / REPLY
     */
    @Column(name = "target_type", length = 10, nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * LIKE / EMPATHY
     */
    @Column(name = "reaction_type", length = 10, nullable = false)
    private String reactionType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
