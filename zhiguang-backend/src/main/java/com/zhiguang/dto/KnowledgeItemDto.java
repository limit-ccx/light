package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeItemDto {
    private Long id;
    private String cachedTitle;
    private String cachedContent;
    private String cachedSummary;
    private String cachedCover;
    private List<String> cachedTags;
    private String cachedAuthorName;
    private String cachedAuthorAvatar;
    private Long originalArticleId;
    private Boolean originalDeleted;
    private Long categoryId;
    private String categoryName;
    private String notes;
    private String readStatus;
    private List<String> customTags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
