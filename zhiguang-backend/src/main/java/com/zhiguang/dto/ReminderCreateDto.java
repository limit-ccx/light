package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReminderCreateDto {

    @NotNull(message = "请选择催办对象")
    private Long toUserId;

    @NotBlank(message = "请填写催办标题")
    @Size(max = 120, message = "标题不能超过 120 字")
    private String title;

    @Size(max = 500, message = "催办说明不能超过 500 字")
    private String content;
}
