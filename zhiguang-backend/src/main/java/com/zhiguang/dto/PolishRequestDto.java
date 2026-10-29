package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PolishRequestDto {

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容不能超过10000字符")
    private String content;

    /**
     * 润色模式：
     * polish  - 智能润色（默认）
     * formal  - 正式化
     * casual  - 轻松化
     * concise - 精简提炼
     * expand  - 扩写丰富
     */
    private String mode = "polish";

    /** 文章标题（可选，提供更好的上下文） */
    private String title;
}
