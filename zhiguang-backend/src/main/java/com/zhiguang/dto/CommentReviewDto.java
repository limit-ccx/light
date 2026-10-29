package com.zhiguang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReviewDto {
    @NotNull(message = "审核状态不能为空")
    private String status; // NORMAL / DELETED
}
