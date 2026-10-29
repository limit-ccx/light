package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.FollowResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.Follow;
import com.zhiguang.entity.User;
import com.zhiguang.service.FollowService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        boolean isFollowing = followService.toggleFollow(user.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "isFollowing", isFollowing,
                "followingCount", followService.getFollowingCount(user.getId()),
                "followerCount", followService.getFollowerCount(userId)
        )));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFollowStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        boolean isFollowing = user != null && followService.isFollowing(user.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "isFollowing", isFollowing,
                "followingCount", followService.getFollowingCount(userId),
                "followerCount", followService.getFollowerCount(userId)
        )));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<PageResponse<FollowResponseDto>>> getFollowingList(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size, PageUtil.MAX_FOLLOW_PAGE_SIZE);
        Page<Follow> result = followService.getFollowingList(user.getId(), page, size);
        List<FollowResponseDto> content = result.getContent().stream()
                .map(FollowResponseDto::fromFollowing)
                .collect(Collectors.toList());
        PageResponse<FollowResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<PageResponse<FollowResponseDto>>> getFollowerList(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size, PageUtil.MAX_FOLLOW_PAGE_SIZE);
        Page<Follow> result = followService.getFollowerList(user.getId(), page, size);
        List<FollowResponseDto> content = result.getContent().stream()
                .map(FollowResponseDto::fromFollower)
                .collect(Collectors.toList());
        PageResponse<FollowResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
