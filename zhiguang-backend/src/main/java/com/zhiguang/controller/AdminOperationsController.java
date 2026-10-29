package com.zhiguang.controller;

import com.zhiguang.dto.AdminUserDto;
import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.service.AdminUserService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 运营管理模块：超级管理员 / 运营管理的专属管理员管理入口。
 * 只处理管理员账号，普通用户治理仍在 /api/admin/users。
 */
@RestController
@RequestMapping("/api/admin/operations")
@RequiredArgsConstructor
public class AdminOperationsController {

    private final AdminUserService adminUserService;

    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> listAdmins(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String position,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.getAdmins(keyword, position, page, size)));
    }

    @PutMapping("/admins/{id}/position")
    public ResponseEntity<ApiResponse<Void>> updatePosition(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        adminUserService.updateAdminPosition(id, body.get("position"), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("职位已更新", null));
    }

    @PostMapping("/admins/{id}/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        String tempPassword = adminUserService.resetPassword(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("密码已重置为随机临时密码",
                Map.of("tempPassword", tempPassword)));
    }

    @PutMapping("/admins/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        adminUserService.updateUserRole(id, body.get("role"), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("角色已更新", null));
    }
}
