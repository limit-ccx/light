package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.CreatorApplicationDto;
import com.zhiguang.dto.CreatorApplicationResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.dto.ReviewDto;
import com.zhiguang.entity.ApplicationStatus;
import com.zhiguang.entity.User;
import com.zhiguang.service.CreatorApplicationService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creator-applications")
@RequiredArgsConstructor
public class CreatorApplicationController {

    private final CreatorApplicationService creatorApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreatorApplicationResponseDto>> apply(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatorApplicationDto dto) {
        CreatorApplicationResponseDto response = creatorApplicationService.apply(user.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("提交成功，请等待审核", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<CreatorApplicationResponseDto>> getMyApplication(
            @AuthenticationPrincipal User user) {
        CreatorApplicationResponseDto response = creatorApplicationService.getMyApplication(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CreatorApplicationResponseDto>>> listApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ApplicationStatus status) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<CreatorApplicationResponseDto> response = creatorApplicationService
                .listApplications(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ApiResponse<CreatorApplicationResponseDto>> review(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ReviewDto dto) {
        CreatorApplicationResponseDto response = creatorApplicationService.review(id, dto, user.getId());
        String message = response.getStatus() == ApplicationStatus.APPROVED ? "审核通过" : "已拒绝";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
