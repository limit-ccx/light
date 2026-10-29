package com.zhiguang.dto;

import com.zhiguang.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewDto {

    @NotNull(message = "审核状态不能为空")
    private ApplicationStatus status;

    @Size(max = 500, message = "审核原因长度不能超过500")
    private String reviewReason;
}
