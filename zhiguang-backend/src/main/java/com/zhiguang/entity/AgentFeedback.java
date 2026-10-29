package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 导览助手回复的用户反馈（赞/踩），管理端可导出迭代提示词。 */
@Entity
@Table(name = "agent_feedback", indexes = {
        @Index(name = "idx_feedback_conversation", columnList = "conversation_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "message_id", length = 64)
    private String messageId;

    /** LIKE / DISLIKE */
    @Column(nullable = false, length = 10)
    private String rating;

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
