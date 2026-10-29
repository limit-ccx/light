package com.zhiguang.service;

import com.zhiguang.entity.RefreshToken;
import com.zhiguang.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String COOKIE_NAME = "refresh_token";
    private static final Duration REMEMBER_LIFETIME = Duration.ofDays(30);
    private static final Duration SESSION_LIFETIME = Duration.ofHours(12);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenResult issue(Long userId, boolean remember, boolean secure) {
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(remember ? REMEMBER_LIFETIME : SESSION_LIFETIME);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setExpiresAt(expiresAt);
        entity.setRemembered(remember);
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        return new RefreshTokenResult(rawToken, buildCookie(rawToken, remember, expiresAt, secure));
    }

    @Transactional
    public RefreshTokenResult validateAndRotate(String rawToken, boolean secure) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String tokenHash = hashToken(rawToken);
        Optional<RefreshToken> optional = refreshTokenRepository.findByTokenHash(tokenHash);
        if (optional.isEmpty()) {
            return null;
        }

        RefreshToken existing = optional.get();
        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(existing.getUserId());
            return null;
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.revokeByTokenHash(tokenHash);
            return null;
        }

        refreshTokenRepository.revokeByTokenHash(tokenHash);
        RefreshTokenResult issued = issue(existing.getUserId(), existing.isRemembered(), secure);
        return new RefreshTokenResult(issued.rawToken(), issued.cookie(), existing.getUserId());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.revokeByTokenHash(hashToken(rawToken));
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public String cookieName() {
        return COOKIE_NAME;
    }

    public String readCookie(HttpCookie cookie) {
        return cookie == null ? null : cookie.getValue();
    }

    public ResponseCookie deletionCookie(boolean secure) {
        return baseCookie("", secure)
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie buildCookie(String value, boolean remember, LocalDateTime expiresAt, boolean secure) {
        ResponseCookie.ResponseCookieBuilder builder = baseCookie(value, secure);
        if (remember) {
            builder.maxAge(Duration.between(LocalDateTime.now(), expiresAt));
        }
        return builder.build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/auth");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public record RefreshTokenResult(String rawToken, ResponseCookie cookie, Long userId) {
        public RefreshTokenResult(String rawToken, ResponseCookie cookie) {
            this(rawToken, cookie, null);
        }
    }
}
