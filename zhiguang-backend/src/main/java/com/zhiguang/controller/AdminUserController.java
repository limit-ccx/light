package com.zhiguang.controller;

import com.zhiguang.dto.AdminUserDto;
import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.security.AdminPermissions;
import com.zhiguang.service.AdminUserService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User admin) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        boolean adminOnly = admin != null
                && AdminPermissions.isAdminManagerOnly(admin.getAdminPosition());
        PageResponse<AdminUserDto> response =
                adminUserService.getUsers(username, email, role, status, adminOnly, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserOverview() {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUserOverview()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDto>> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUserDetail(userId)));
    }

    @PostMapping("/{userId}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable Long userId, @AuthenticationPrincipal User admin) {
        adminUserService.banUser(userId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("用户已封禁", null));
    }

    @PostMapping("/{userId}/unban")
    public ResponseEntity<ApiResponse<Void>> unbanUser(@PathVariable Long userId, @AuthenticationPrincipal User admin) {
        adminUserService.unbanUser(userId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("用户已解封", null));
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable Long userId, @AuthenticationPrincipal User admin) {
        String tempPassword = adminUserService.resetPassword(userId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("密码已重置为随机临时密码", Map.of("tempPassword", tempPassword)));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        String role = body.get("role");
        adminUserService.updateUserRole(userId, role, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("角色已更新", null));
    }

    @PutMapping("/{userId}/position")
    public ResponseEntity<ApiResponse<Void>> updatePosition(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        adminUserService.updateAdminPosition(userId, body.get("position"), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("职位已更新", null));
    }
}
