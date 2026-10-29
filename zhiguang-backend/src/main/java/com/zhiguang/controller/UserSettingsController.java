package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.entity.User;
import com.zhiguang.entity.UserSettings;
import com.zhiguang.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsRepository userSettingsRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<UserSettings>> getSettings(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserSettings>> updateSettings(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }

        UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElse(new UserSettings());
        settings.setUserId(user.getId());

        if (body.containsKey("innerVoiceConfig")) {
            Object raw = body.get("innerVoiceConfig");
            String config = raw == null ? null : String.valueOf(raw);
            if (config != null && config.length() > 20000) {
                return ResponseEntity.ok(ApiResponse.error(400, "心声配置过大"));
            }
            settings.setInnerVoiceConfig(config);
        }

        userSettingsRepository.save(settings);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
}
