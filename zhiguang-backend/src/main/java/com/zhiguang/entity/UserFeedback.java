package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** 用户建议反馈：前台提交，运营后台按职位同步处理。 */
@Entity
@Table(name = "user_feedback", indexes = {
        @Index(name = "idx_user_feedback_status", columnList = "status"),
        @Index(name = "idx_user_feedback_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    /** bug / feature / content / other */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 120)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 120)
    private String contact;

    /** PENDING / HANDLED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String reply;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
