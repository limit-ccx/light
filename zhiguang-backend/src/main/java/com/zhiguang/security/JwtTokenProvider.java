package com.zhiguang.security;

import com.zhiguang.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String ISSUER = "zhiguang-platform";
    private static final String CLAIM_SECURITY_VERSION = "ver";
    /** 随仓库公开的默认密钥（application.yml 兜底值），生产环境使用即视为未配置 */
    private static final String KNOWN_DEFAULT_SECRET =
            "ZhiguangPlatformSecretKey2024ThisIsAVeryLongSecretKeyForJWTTokenGeneration";

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            org.springframework.core.env.Environment environment) {
        // fail-fast：生产 profile 下密钥缺失或为仓库公开的默认值时拒绝启动，
        // 否则攻击者可用默认密钥自签任意用户 token 完全绕过认证
        boolean prodProfile = environment != null
                && environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"));
        boolean usingDefault = secret == null || secret.isBlank() || KNOWN_DEFAULT_SECRET.equals(secret);
        if (prodProfile && usingDefault) {
            throw new IllegalStateException(
                    "生产环境必须通过 JWT_SECRET 环境变量设置强随机密钥（当前为空或使用仓库公开的默认值）");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(User user) {
        int securityVersion = user.getSecurityVersion() == null ? 0 : user.getSecurityVersion();
        return generateToken(user.getId(), user.getUsername(), securityVersion);
    }

    private String generateToken(Long userId, String username, int securityVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuer(ISSUER)
                .claim("username", username)
                .claim(CLAIM_SECURITY_VERSION, securityVersion)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public int getSecurityVersion(Claims claims) {
        Number value = claims.get(CLAIM_SECURITY_VERSION, Number.class);
        return value == null ? -1 : value.intValue();
    }

    public String getTokenId(Claims claims) {
        return claims.getId();
    }

    public long getRemainingExpiration(Claims claims) {
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public Long getUserIdFromToken(String token) {
        return getUserId(parseClaims(token));
    }

    public String getUsernameFromToken(String token) {
        return getUsername(parseClaims(token));
    }

    public String getTokenId(String token) {
        return getTokenId(parseClaims(token));
    }

    public long getRemainingExpiration(String token) {
        return getRemainingExpiration(parseClaims(token));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
