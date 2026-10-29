package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.FeedbackSubmitDto;
import com.zhiguang.entity.User;
import com.zhiguang.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FeedbackSubmitDto dto) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("请先登录后再提交反馈"));
        }
        feedbackService.submit(user.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("感谢反馈，我们会尽快处理", null));
    }
}
