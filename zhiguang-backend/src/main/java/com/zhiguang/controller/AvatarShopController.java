package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.entity.User;
import com.zhiguang.service.AvatarShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/avatar-shop")
@RequiredArgsConstructor
public class AvatarShopController {

    private final AvatarShopService avatarShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShop(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        return ResponseEntity.ok(ApiResponse.success(avatarShopService.getShop(user.getId())));
    }

    @PostMapping("/{avatarId}/purchase")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purchase(
            @AuthenticationPrincipal User user,
            @PathVariable String avatarId) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        return ResponseEntity.ok(ApiResponse.success("兑换成功", avatarShopService.purchase(user.getId(), avatarId)));
    }

    @PostMapping("/{avatarId}/equip")
    public ResponseEntity<ApiResponse<Map<String, Object>>> equip(
            @AuthenticationPrincipal User user,
            @PathVariable String avatarId) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        return ResponseEntity.ok(ApiResponse.success("头像已更新", avatarShopService.equip(user.getId(), avatarId)));
    }
}
