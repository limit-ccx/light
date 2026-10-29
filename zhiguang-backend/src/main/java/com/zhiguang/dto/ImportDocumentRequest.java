package com.zhiguang.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportDocumentRequest {
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private String notes;
    private List<String> customTags;
}
