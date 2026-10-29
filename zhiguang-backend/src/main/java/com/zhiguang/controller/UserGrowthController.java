package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.SignInCalendarDto;
import com.zhiguang.dto.SignInResultDto;
import com.zhiguang.entity.PointLog;
import com.zhiguang.entity.User;
import com.zhiguang.entity.UserLevel;
import com.zhiguang.entity.UserPoint;
import com.zhiguang.service.SignInService;
import com.zhiguang.service.UserGrowthService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/growth")
@RequiredArgsConstructor
public class UserGrowthController {

    private final UserGrowthService userGrowthService;
    private final SignInService signInService;

    private static final int BASE_EXP_PER_LEVEL = 100;

    @GetMapping("/level")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLevel(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        UserLevel userLevel = userGrowthService.getUserLevel(user.getId());
        int nextLevelExperience = BASE_EXP_PER_LEVEL * userLevel.getLevel() * (userLevel.getLevel() + 1) / 2;

        Map<String, Object> result = new HashMap<>();
        result.put("level", userLevel.getLevel());
        result.put("experience", userLevel.getExperience());
        result.put("nextLevelExperience", nextLevelExperience);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAchievements(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        List<Map<String, Object>> achievements = userGrowthService.getAchievementsWithStatus(user.getId());
        long obtainedCount = achievements.stream().filter(a -> Boolean.TRUE.equals(a.get("obtained"))).count();

        Map<String, Object> result = new HashMap<>();
        result.put("achievements", achievements);
        result.put("obtainedCount", obtainedCount);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<SignInResultDto>> signIn(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        SignInResultDto result = signInService.signIn(user.getId());
        return ResponseEntity.ok(ApiResponse.success("签到成功", result));
    }

    @PostMapping("/sign-in/supplement")
    public ResponseEntity<ApiResponse<SignInResultDto>> supplementSignIn(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        SignInResultDto result = signInService.supplementSignIn(user.getId(), date);
        return ResponseEntity.ok(ApiResponse.success("补签成功", result));
    }

    @GetMapping("/sign-in/calendar")
    public ResponseEntity<ApiResponse<SignInCalendarDto>> getSignInCalendar(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        SignInCalendarDto result = signInService.getCalendar(user.getId(), y, m);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/sign-in/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSignInStatus(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        Map<String, Object> result = signInService.getSignInStatus(user.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/points")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPoints(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        UserPoint userPoint = userGrowthService.getUserPoints(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("points", userPoint.getPoints());
        result.put("totalPoints", userPoint.getTotalPoints());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/points/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPointLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);

        List<PointLog> logs = userGrowthService.getPointLogs(user.getId(), page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
