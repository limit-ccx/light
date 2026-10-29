package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponseDto {
    private Long id;
    private Long fromUserId;
    private String fromUsername;
    private Long toUserId;
    private String toUsername;
    private String title;
    private String content;
    private String status;
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime completedAt;
    private LocalDateTime acknowledgedAt;
}
