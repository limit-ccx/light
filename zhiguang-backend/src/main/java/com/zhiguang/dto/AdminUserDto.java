package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private String bio;
    private String role;
    private String adminPosition;
    private String status;
    private List<String> creatorBadges;
    private long articleCount;
    private long commentCount;
    private long followerCount;
    private LocalDateTime createdAt;
}
