package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponseDto {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private String cover;
    private AuthorDto author;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private List<String> tags;
    private Boolean isLiked;
    private Boolean isFavorited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String aiSummary;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String username;
        private String avatar;
        private String bio;
    }
}