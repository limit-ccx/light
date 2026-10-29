package com.zhiguang.dto;

import com.zhiguang.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorApplicationResponseDto {

    private Long id;
    private Long userId;
    private String direction;
    private String bio;
    private List<String> works;
    private ApplicationStatus status;
    private String reviewReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
