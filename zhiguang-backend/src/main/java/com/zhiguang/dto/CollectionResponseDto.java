package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponseDto {

    private Long id;
    private String name;
    private String description;
    private ColumnResponseDto.UserBriefDto author;
    private Integer articleCount;
    private LocalDateTime createdAt;
}
