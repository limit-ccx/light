package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补签记录
 */
@Entity
@Table(name = "sign_in_supplements", indexes = {
        @Index(name = "idx_supplement_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "month_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInSupplement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "month_key", nullable = false, length = 7)
    private String monthKey;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "max_count", nullable = false)
    private Integer maxCount = 3;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getRemainingCount() {
        return Math.max(0, maxCount - usedCount);
    }
}
