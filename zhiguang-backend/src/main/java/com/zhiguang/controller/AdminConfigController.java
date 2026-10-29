package com.zhiguang.controller;

import com.zhiguang.dto.*;
import com.zhiguang.entity.Announcement;
import com.zhiguang.entity.Banner;
import com.zhiguang.entity.User;
import com.zhiguang.service.AdminConfigService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    // ========== 系统配置 ==========

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<List<SystemConfigDto>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.success(adminConfigService.getAllConfigs()));
    }

    @PutMapping("/system/{key}")
    public ResponseEntity<ApiResponse<Void>> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        adminConfigService.updateConfig(key, body.get("value"), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("配置已更新", null));
    }

    @PutMapping("/system/batch")
    public ResponseEntity<ApiResponse<Void>> batchUpdateConfigs(
            @RequestBody Map<String, String> configs,
            @AuthenticationPrincipal User admin) {
        adminConfigService.batchUpdateConfigs(configs, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("批量配置已更新", null));
    }

    // ========== 公告管理 ==========

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<PageResponse<Announcement>>> getAnnouncements(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<Announcement> response = adminConfigService.getAnnouncements(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<Announcement>> createAnnouncement(
            @Valid @RequestBody AnnouncementRequestDto dto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.success("发布公告成功", adminConfigService.createAnnouncement(dto, admin.getId())));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Announcement>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequestDto dto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", adminConfigService.updateAnnouncement(id, dto, admin.getId())));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        adminConfigService.deleteAnnouncement(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ========== Banner 管理 ==========

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<PageResponse<Banner>>> getBanners(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<Banner> response = adminConfigService.getBanners(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/banners")
    public ResponseEntity<ApiResponse<Banner>> createBanner(
            @Valid @RequestBody BannerRequestDto dto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.success("创建成功", adminConfigService.createBanner(dto, admin.getId())));
    }

    @PutMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<Banner>> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody BannerRequestDto dto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", adminConfigService.updateBanner(id, dto, admin.getId())));
    }

    @PutMapping("/banners/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateBannerStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        adminConfigService.updateBannerStatus(id, body.get("status"), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("状态已更新", null));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        adminConfigService.deleteBanner(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ========== 公开接口 ==========

    @GetMapping("/public/announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> getPublishedAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success(adminConfigService.getPublishedAnnouncements()));
    }

    @GetMapping("/public/banners")
    public ResponseEntity<ApiResponse<List<Banner>>> getActiveBanners(
            @RequestParam(defaultValue = "HOME") String position) {
        return ResponseEntity.ok(ApiResponse.success(adminConfigService.getActiveBanners(position)));
    }
}
