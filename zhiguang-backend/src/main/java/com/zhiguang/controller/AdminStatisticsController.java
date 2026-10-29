package com.zhiguang.controller;

import com.zhiguang.dto.AdminDashboardDto;
import com.zhiguang.dto.ApiResponse;
import com.zhiguang.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminStatisticsService.getDashboard()));
    }

    @GetMapping("/user-growth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserGrowth(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(adminStatisticsService.getUserGrowth(days)));
    }

    @GetMapping("/content")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContentStats() {
        return ResponseEntity.ok(ApiResponse.success(adminStatisticsService.getContentStats()));
    }
}
