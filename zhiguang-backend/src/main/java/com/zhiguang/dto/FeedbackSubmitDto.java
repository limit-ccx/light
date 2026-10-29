package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackSubmitDto {

    @NotBlank(message = "请选择反馈类型")
    @Size(max = 20, message = "反馈类型过长")
    private String type;

    @Size(max = 120, message = "标题不能超过 120 字")
    private String title;

    @NotBlank(message = "请填写反馈内容")
    @Size(max = 2000, message = "反馈内容不能超过 2000 字")
    private String content;

    @Size(max = 120, message = "联系方式过长")
    private String contact;
}
