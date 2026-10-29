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
public class PublicUserDto {
    private Long id;
    private String username;
    private String avatar;
    private String bio;
    private List<String> creatorBadges = new ArrayList<>();
    private LocalDateTime createdAt;
}
