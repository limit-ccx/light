package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.NotificationResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.Notification;
import com.zhiguang.entity.User;
import com.zhiguang.service.NotificationService;
import com.zhiguang.service.SseEmitterService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping("/stream")
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        if (user == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("未登录或登录已过期"));
            return emitter;
        }
        return sseEmitterService.subscribe(user.getId(), com.zhiguang.service.SseEmitterService.CHANNEL_NOTIFICATION);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        Page<Notification> result = notificationService.getUserNotifications(user.getId(), page, size);
        List<NotificationResponseDto> content = result.getContent().stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
        PageResponse<NotificationResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = user == null ? 0L : notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
