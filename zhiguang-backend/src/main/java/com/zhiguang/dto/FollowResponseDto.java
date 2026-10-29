package com.zhiguang.dto;

import com.zhiguang.entity.Follow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponseDto {

    private Long id;
    private UserBriefDto user;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBriefDto {
        private Long id;
        private String username;
        private String avatar;
        private String bio;
    }

    /**
     * 从 Follow 实体创建 DTO（用于关注列表，返回被关注的人）
     */
    public static FollowResponseDto fromFollowing(Follow follow) {
        UserBriefDto userDto = new UserBriefDto(
                follow.getFollowing().getId(),
                follow.getFollowing().getUsername(),
                follow.getFollowing().getAvatar(),
                follow.getFollowing().getBio()
        );
        return new FollowResponseDto(follow.getId(), userDto, follow.getCreatedAt());
    }

    /**
     * 从 Follow 实体创建 DTO（用于粉丝列表，返回粉丝）
     */
    public static FollowResponseDto fromFollower(Follow follow) {
        UserBriefDto userDto = new UserBriefDto(
                follow.getFollower().getId(),
                follow.getFollower().getUsername(),
                follow.getFollower().getAvatar(),
                follow.getFollower().getBio()
        );
        return new FollowResponseDto(follow.getId(), userDto, follow.getCreatedAt());
    }
}