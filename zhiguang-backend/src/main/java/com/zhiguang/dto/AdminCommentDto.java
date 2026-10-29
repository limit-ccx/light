package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCommentDto {
    private Long id;
    private String content;
    private Long articleId;
    private String articleTitle;
    private Long userId;
    private String username;
    private String status;
    private LocalDateTime createdAt;
}
