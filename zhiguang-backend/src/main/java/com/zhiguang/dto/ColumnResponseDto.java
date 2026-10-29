package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnResponseDto {

    private Long id;
    private String name;
    private String description;
    private String cover;
    private UserBriefDto author;
    private Integer subscriberCount;
    private Integer articleCount;
    private Boolean isSubscribed;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBriefDto {
        private Long id;
        private String username;
        private String avatar;
    }
}
