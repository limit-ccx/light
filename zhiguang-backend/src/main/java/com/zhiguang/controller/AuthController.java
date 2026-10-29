package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.AuthResponseDto;
import com.zhiguang.dto.LoginRequestDto;
import com.zhiguang.dto.RegisterRequestDto;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.security.JwtTokenProvider;
import com.zhiguang.security.SseTicketService;
import com.zhiguang.security.TokenBlacklistService;
import com.zhiguang.service.RefreshTokenService;
import com.zhiguang.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SseTicketService sseTicketService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest servletRequest) {
        AuthResponseDto response = userService.register(request);
        RefreshTokenService.RefreshTokenResult refresh = refreshTokenService.issue(
                response.getUser().getId(), true, servletRequest.isSecure());
        return withCookie(ResponseEntity.ok(ApiResponse.success("注册成功", response)), refresh.cookie());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest servletRequest) {
        AuthResponseDto response = userService.login(request);
        boolean remember = request.isRemember();
        RefreshTokenService.RefreshTokenResult refresh = refreshTokenService.issue(
                response.getUser().getId(), remember, servletRequest.isSecure());
        return withCookie(ResponseEntity.ok(ApiResponse.success("登录成功", response)), refresh.cookie());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refresh(
            HttpServletRequest servletRequest,
            @CookieValue(name = "refresh_token", required = false) String refreshCookie) {
        String rawToken = getCookieValue(refreshCookie, servletRequest);
        RefreshTokenService.RefreshTokenResult rotated = refreshTokenService.validateAndRotate(rawToken, servletRequest.isSecure());
        if (rotated == null || rotated.userId() == null) {
            ResponseCookie deletion = refreshTokenService.deletionCookie(servletRequest.isSecure());
            return withCookie(ResponseEntity.status(401).body(ApiResponse.error(401, "登录状态已失效，请重新登录")), deletion);
        }

        AuthResponseDto response = userService.issueAuthResponseByUserId(rotated.userId());
        return withCookie(ResponseEntity.ok(ApiResponse.success("刷新成功", response)), rotated.cookie());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @CookieValue(name = "refresh_token", required = false) String refreshCookie) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(bearerToken.substring(7));
                tokenBlacklistService.blacklist(
                        jwtTokenProvider.getTokenId(claims),
                        jwtTokenProvider.getRemainingExpiration(claims)
                );
            } catch (Exception ignored) {
                // Token 无效或已过期时只需清除会话 Cookie
            }
        }

        String rawRefresh = getCookieValue(refreshCookie, request);
        if (rawRefresh != null && !rawRefresh.isBlank()) {
            refreshTokenService.revoke(rawRefresh);
        }

        return withCookie(ResponseEntity.ok(ApiResponse.success("已登出", null)),
                refreshTokenService.deletionCookie(request.isSecure()));
    }

    @PostMapping("/sse-ticket")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSseTicket(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BadRequestException("请先登录");
        }
        String ticket = sseTicketService.generateTicket(user.getId());
        return ResponseEntity.ok(ApiResponse.success("ticket 已生成", Map.of("ticket", ticket)));
    }

    private String getCookieValue(String cookieValue, HttpServletRequest request) {
        if (StringUtils.hasText(cookieValue)) {
            return cookieValue;
        }
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie candidate : request.getCookies()) {
            if ("refresh_token".equals(candidate.getName())) {
                return candidate.getValue();
            }
        }
        return null;
    }

    private <T> ResponseEntity<T> withCookie(ResponseEntity<T> response, ResponseCookie cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(response.getHeaders());
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }
}
