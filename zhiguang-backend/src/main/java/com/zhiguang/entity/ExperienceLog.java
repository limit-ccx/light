package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "experience_logs", indexes = {
        @Index(name = "idx_experience_log_user", columnList = "user_id"),
        @Index(name = "idx_experience_log_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Integer balance;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
