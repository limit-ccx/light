package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {

    private Long id;
    private String content;
    private ArticleBriefDto article;
    private UserBriefDto user;
    private UserBriefDto parent;
    private LocalDateTime createdAt;
    private Boolean isLiked;
    private Long likeCount;
    private List<CommentResponseDto> replies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleBriefDto {
        private Long id;
        private String title;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBriefDto {
        private Long id;
        private String username;
        private String avatar;
    }
}