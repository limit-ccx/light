package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.FeedbackResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.service.FeedbackService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponseDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        return ResponseEntity.ok(ApiResponse.success(feedbackService.page(status, page, size)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.stats()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> handle(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        feedbackService.handle(id, body.get("status"), body.get("reply"));
        return ResponseEntity.ok(ApiResponse.success("反馈已更新", null));
    }
}
