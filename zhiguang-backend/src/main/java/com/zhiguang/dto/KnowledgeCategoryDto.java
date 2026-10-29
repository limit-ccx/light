package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCategoryDto {
    private Long id;
    private String name;
    private Long parentId;
    private String icon;
    private String color;
    private Integer sortOrder;
    private List<KnowledgeCategoryDto> children;
    private LocalDateTime createdAt;
}
