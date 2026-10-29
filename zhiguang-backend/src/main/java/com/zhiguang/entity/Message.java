package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_client_message_id", columnList = "client_message_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "client_message_id", unique = true, length = 64)
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "is_read")
    private Boolean isRead = false;

    /** 仅对发送者删除（原始类型 + 非空：DDL 迁移时旧行自动填 false，避免 NULL 匹配问题） */
    @Column(name = "deleted_for_sender", nullable = false)
    private boolean deletedForSender = false;

    /** 仅对接收者删除 */
    @Column(name = "deleted_for_receiver", nullable = false)
    private boolean deletedForReceiver = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
