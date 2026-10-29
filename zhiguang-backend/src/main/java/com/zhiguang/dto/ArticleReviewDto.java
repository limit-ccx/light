package com.zhiguang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReviewDto {
    @NotNull(message = "审核状态不能为空")
    private Integer status; // 2=通过 4=下架
    private String reason;
}
