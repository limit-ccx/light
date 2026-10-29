package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponseDto {
    private Long id;
    private Long userId;
    private String username;
    private String type;
    private String title;
    private String content;
    private String contact;
    private String status;
    private String reply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
