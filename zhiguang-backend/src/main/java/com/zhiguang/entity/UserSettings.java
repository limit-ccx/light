package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户个性化设置
 */
@Entity
@Table(name = "user_settings", uniqueConstraints = {
    @UniqueConstraint(name = "uk_settings_user", columnNames = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** 悬浮球“心声”配置（JSON：voices / selectedIds / enabled / interval） */
    @Column(name = "inner_voice_config", columnDefinition = "TEXT")
    private String innerVoiceConfig;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
