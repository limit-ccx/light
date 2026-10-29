package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 后台催办单：超级管理员发起，运营管理接收并回执完成。 */
@Entity
@Table(name = "admin_reminder", indexes = {
        @Index(name = "idx_reminder_to", columnList = "to_user_id,status"),
        @Index(name = "idx_reminder_from", columnList = "from_user_id,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String content;

    /** PENDING / DONE */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    /** 接收方完成时填写的回执 */
    @Column(length = 500)
    private String reply;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 接收方阅读时间，用于后台消息提示的未读角标 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 发起方确认收到回执的时间，用于清掉催办角标 */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
}
