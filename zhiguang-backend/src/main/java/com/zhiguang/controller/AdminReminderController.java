package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.dto.ReminderCreateDto;
import com.zhiguang.dto.ReminderResponseDto;
import com.zhiguang.entity.User;
import com.zhiguang.service.AdminReminderService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 催办：超级管理员发起，运营管理接收并回执完成。 */
@RestController
@RequestMapping("/api/admin/reminders")
@RequiredArgsConstructor
public class AdminReminderController {

    private final AdminReminderService reminderService;

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<PageResponse<ReminderResponseDto>>> received(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        return ResponseEntity.ok(ApiResponse.success(
                reminderService.received(user.getId(), status, page, size)));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PageResponse<ReminderResponseDto>>> sent(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        return ResponseEntity.ok(ApiResponse.success(
                reminderService.sent(user.getId(), status, page, size)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reminderService.stats(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReminderCreateDto dto) {
        reminderService.create(user, dto);
        return ResponseEntity.ok(ApiResponse.success("催办已发送", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal User user) {
        reminderService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success("已全部标记为已读", null));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        reminderService.complete(id, user.getId(), body.get("reply"));
        return ResponseEntity.ok(ApiResponse.success("已回执完成", null));
    }

    @PutMapping("/{id}/ack")
    public ResponseEntity<ApiResponse<Void>> acknowledge(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        reminderService.acknowledge(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("已确认", null));
    }
}
