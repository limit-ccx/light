package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSnapshot {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private String bio;
    private String role;
    private String adminPosition;
    private String status;
    private List<String> creatorBadges = new ArrayList<>();
    private Integer securityVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
