package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveWordDto {
    @NotBlank(message = "敏感词不能为空")
    private String word;
    private String category;
}
