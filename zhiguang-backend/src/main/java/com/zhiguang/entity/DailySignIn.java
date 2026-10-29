package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_sign_ins", indexes = {
        @Index(name = "idx_daily_sign_in_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "sign_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySignIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sign_date", nullable = false)
    private LocalDate signDate;

    @Column(name = "continuous_days", nullable = false)
    private Integer continuousDays = 1;

    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints = 0;

    @Column(name = "reward_experience", nullable = false)
    private Integer rewardExperience = 0;

    @Column(name = "is_supplement", nullable = false)
    private Boolean supplement = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
