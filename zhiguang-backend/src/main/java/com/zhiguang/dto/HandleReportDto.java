package com.zhiguang.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandleReportDto {
    @NotNull(message = "处理状态不能为空")
    private String status; // HANDLED / DISMISSED
    private String handleResult;
}
