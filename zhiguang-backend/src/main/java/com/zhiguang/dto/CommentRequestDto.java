package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequestDto {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;

    private Long articleId;
}