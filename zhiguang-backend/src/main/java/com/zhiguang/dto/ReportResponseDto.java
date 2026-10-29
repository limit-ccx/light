package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String reason;
    private String status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
