package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户已兑换的头像
 */
@Entity
@Table(name = "user_avatar_unlocks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_avatar_unlock_user_avatar", columnNames = {"user_id", "avatar_id"})
}, indexes = {
        @Index(name = "idx_avatar_unlock_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatarUnlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "avatar_id", nullable = false, length = 50)
    private String avatarId;

    @CreationTimestamp
    @Column(name = "unlocked_at", updatable = false)
    private LocalDateTime unlockedAt;
}
