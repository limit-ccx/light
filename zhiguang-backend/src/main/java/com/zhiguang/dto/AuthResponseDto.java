package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private UserDto user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String avatar;
        private String bio;
        private List<String> creatorBadges;
        private String role;
        private String adminPosition;
        private String status;
    }
}