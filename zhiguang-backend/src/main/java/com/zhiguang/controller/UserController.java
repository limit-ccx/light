package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.AuthResponseDto;
import com.zhiguang.dto.PublicUserDto;
import com.zhiguang.dto.UserUpdateDto;
import com.zhiguang.entity.User;
import com.zhiguang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        AuthResponseDto.UserDto userDto = userService.getUserDto(user.getId());
        Map<String, Object> stats = userService.getUserStats(user.getId());
        
        Map<String, Object> result = Map.of(
                "user", userDto,
                "articleCount", stats.get("articleCount"),
                "followCount", stats.get("followCount"),
                "fansCount", stats.get("fansCount")
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponseDto.UserDto>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UserUpdateDto updateDto) {
        User updatedUser = userService.updateUser(user.getId(), updateDto);
        AuthResponseDto.UserDto userDto = userService.buildUserDto(updatedUser);
        return ResponseEntity.ok(ApiResponse.success("更新成功", userDto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        PublicUserDto userDto = userService.getPublicUserDto(userId);
        Map<String, Object> stats = userService.getUserStats(userId);
        
        Map<String, Object> result = Map.of(
                "user", userDto,
                "articleCount", stats.get("articleCount"),
                "followCount", stats.get("followCount"),
                "fansCount", stats.get("fansCount")
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}