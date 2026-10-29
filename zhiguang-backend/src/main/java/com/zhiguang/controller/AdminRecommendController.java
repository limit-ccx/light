package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.FeaturedContentDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.FeaturedContent;
import com.zhiguang.entity.User;
import com.zhiguang.service.AdminRecommendService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommend")
@RequiredArgsConstructor
public class AdminRecommendController {

    private final AdminRecommendService adminRecommendService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeaturedContent>>> getFeaturedContent(
            @RequestParam(required = false) String position,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<FeaturedContent> response = adminRecommendService.getFeaturedContent(position, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeaturedContent>> addFeaturedContent(
            @Valid @RequestBody FeaturedContentDto dto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.success("添加成功", adminRecommendService.addFeaturedContent(dto, admin.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFeaturedContent(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        adminRecommendService.removeFeaturedContent(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("已移除", null));
    }

    // ========== 公开接口 ==========

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<FeaturedContent>>> getPublicFeatured(
            @RequestParam(defaultValue = "HOME") String position) {
        return ResponseEntity.ok(ApiResponse.success(adminRecommendService.getPublicFeatured(position)));
    }
}
