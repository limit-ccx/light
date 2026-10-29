package com.zhiguang.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 树洞匿名身份签发与验证（SEC-P1-5 修复）。
 * <p>
 * 此前匿名 id 完全由客户端自报（本地 UUID 明文口令），可随意轮换绕过发帖限流，
 * 且拿到他人 id 即可冒充发言、删除其内容。现在：
 * - 身份 token 由服务端签发并带 HMAC 签名与有效期（格式 zg1.<anonId>.<expiry>.<sig>），
 *   客户端无法伪造或枚举，轮换需走签发接口（接口按 IP 限频）；
 * - token 可"无损续期"：验签通过后按同一 anonId 重新签名，持有者对旧内容的
 *   属主关系（服务端存 SHA-256(anonId)）保持不变；
 * - 过渡期兼容旧格式 UUID（legacyAccepted 开关），配合 IP 维度写限流兜底，
 *   待存量用户自然切换后关闭。
 */
@Slf4j
@Service
public class TreeHoleIdentityService {

    public static final String TOKEN_PREFIX = "zg1.";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern LEGACY_UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final String secret;
    private final long ttlMillis;
    private final boolean legacyAccepted;

    public TreeHoleIdentityService(
            @Value("${jwt.secret}") String secret,
            @Value("${tree-hole.identity-ttl-days:30}") long ttlDays,
            @Value("${tree-hole.legacy-identity-accepted:true}") boolean legacyAccepted) {
        this.secret = secret;
        this.ttlMillis = ttlDays * 24L * 3600 * 1000;
        this.legacyAccepted = legacyAccepted;
    }

    /** 签发全新匿名身份 */
    public IssuedIdentity issue() {
        return sign(UUID.randomUUID().toString());
    }

    /**
     * 无损续期：当前 token 验签通过后，对同一 anonId 重新签名延长有效期，
     * 属主关系不变；无效 token 视为新身份签发。
     */
    public IssuedIdentity refreshOrIssue(String rawToken) {
        if (rawToken != null && rawToken.startsWith(TOKEN_PREFIX)) {
            ResolvedIdentity resolved = resolve(rawToken);
            if (resolved.valid()) {
                return sign(resolved.anonId());
            }
        }
        return issue();
    }

    /**
     * 解析客户端提交的身份：签名 token 验签 + 有效期；
     * 旧 UUID 格式过渡期放行；其余一律无效。
     */
    public ResolvedIdentity resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return ResolvedIdentity.invalid();
        }
        if (raw.startsWith(TOKEN_PREFIX)) {
            String[] parts = raw.split("\\.");
            if (parts.length != 4) {
                return ResolvedIdentity.invalid();
            }
            String anonId = parts[1];
            String expiry = parts[2];
            String sig = parts[3];
            if (!MessageDigest.isEqual(
                    hmac(anonId + "." + expiry).getBytes(StandardCharsets.UTF_8),
                    sig.getBytes(StandardCharsets.UTF_8))) {
                log.debug("树洞身份签名校验失败");
                return ResolvedIdentity.invalid();
            }
            long expiresAt;
            try {
                expiresAt = Long.parseLong(expiry);
            } catch (NumberFormatException e) {
                return ResolvedIdentity.invalid();
            }
            if (System.currentTimeMillis() > expiresAt) {
                log.debug("树洞身份已过期");
                return ResolvedIdentity.invalid();
            }
            if (!LEGACY_UUID.matcher(anonId).matches()) {
                // 签名内容必须是服务端签发的 UUID，防御签名覆盖任意字符串
                return ResolvedIdentity.invalid();
            }
            return new ResolvedIdentity(anonId, false, true);
        }
        if (legacyAccepted && LEGACY_UUID.matcher(raw).matches()) {
            return new ResolvedIdentity(raw, true, true);
        }
        return ResolvedIdentity.invalid();
    }

    /** 服务端统一存储/比对用的身份摘要（SHA-256 Base64） */
    public String hash(String anonId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(anonId.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private IssuedIdentity sign(String anonId) {
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        String sig = hmac(anonId + "." + expiresAt);
        String token = TOKEN_PREFIX + anonId + "." + expiresAt + "." + sig;
        return new IssuedIdentity(token, anonId, expiresAt);
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    /** 签发结果 */
    public record IssuedIdentity(String token, String anonId, long expiresAt) {
    }

    /** 解析结果：valid=false 表示不可用于写操作 */
    public record ResolvedIdentity(String anonId, boolean legacy, boolean valid) {
        static ResolvedIdentity invalid() {
            return new ResolvedIdentity(null, false, false);
        }
    }
}
