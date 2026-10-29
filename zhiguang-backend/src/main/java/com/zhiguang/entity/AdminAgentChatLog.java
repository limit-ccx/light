package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 管理端 Agent 对话历史（持久化，跨会话可回看）。 */
@Entity
@Table(name = "admin_agent_chat_log", indexes = {
        @Index(name = "idx_agent_log_admin_time", columnList = "admin_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAgentChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /** user / assistant */
    @Column(nullable = false, length = 16)
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 使用的模型（provider/model）与思考强度 */
    @Column(length = 60)
    private String model;

    @Column(length = 16)
    private String effort;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
