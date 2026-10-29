package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.AdminLog;
import com.zhiguang.service.AdminLogService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminLog>>> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        Page<AdminLog> logPage = adminLogService.getLogs(action, startDate, endDate, page, size);
        PageResponse<AdminLog> response = new PageResponse<>(
                logPage.getContent(), logPage.getTotalElements(),
                logPage.getNumber(), logPage.getSize(), logPage.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
