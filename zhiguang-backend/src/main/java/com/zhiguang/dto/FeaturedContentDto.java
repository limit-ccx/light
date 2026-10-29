package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedContentDto {
    @NotBlank(message = "目标类型不能为空")
    private String targetType;
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
    private String position = "HOME";
    private Integer sortOrder = 0;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
