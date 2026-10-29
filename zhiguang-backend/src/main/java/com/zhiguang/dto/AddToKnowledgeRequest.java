package com.zhiguang.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddToKnowledgeRequest {
    private Long articleId;
    private Long categoryId;
    private String notes;
    private List<String> customTags;
}
