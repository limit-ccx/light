package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleVersionDto {

    private Long id;
    private Long articleId;
    private String title;
    private String content;
    private Integer version;
    private LocalDateTime createdAt;
}
