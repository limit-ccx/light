package com.zhiguang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final Map<String, String> DEFAULT_ENDPOINTS = Map.ofEntries(
            Map.entry("/api/auth/login", "5,60"),
            Map.entry("/api/auth/register", "5,3600"),
            Map.entry("/api/auth/refresh", "30,60"),
            Map.entry("/api/auth/logout", "30,60"),
            Map.entry("/api/auth/sse-ticket", "60,60"),
            Map.entry("/api/articles/*/like", "10,60"),
            Map.entry("/api/comments", "10,60"),
            Map.entry("/api/search", "30,60"),
            // 树洞只限流写操作：读列表/星图每次进页面就会打满旧的 5 次/小时
            Map.entry("POST /api/tree-hole/posts", "5,3600"),
            Map.entry("POST /api/tree-hole/replies", "10,3600"),
            Map.entry("POST /api/tree-hole/reactions", "30,600"),
            Map.entry("POST /api/tree-hole/reports", "5,3600"),
            Map.entry("DELETE /api/tree-hole/**", "20,3600"),
            Map.entry("/api/admin/users/*/ban", "20,60"),
            Map.entry("/api/admin/users/*/unban", "20,60"),
            Map.entry("/api/admin/users/*/reset-password", "10,60"),
            Map.entry("/api/admin/users/*/role", "10,60"),
            Map.entry("/api/admin/content/articles/*/review", "30,60"),
            Map.entry("/api/admin/content/comments/*/review", "30,60"),
            Map.entry("/api/admin/content/reports/*/handle", "30,60"),
            Map.entry("/api/admin/tree-hole/posts/*/approve", "30,60"),
            Map.entry("/api/admin/tree-hole/posts/*/reject", "30,60"),
            Map.entry("/api/admin/tree-hole/posts", "20,60"),
            Map.entry("/api/admin/tree-hole/reports/*/resolve", "30,60")
    );

    private boolean enabled = true;
    private int defaultLimit = 100;
    private int defaultWindow = 60;
    private Map<String, String> endpoints = new HashMap<>();

    /**
     * 规则可写成 "/api/xxx"（任意方法）或 "POST /api/xxx"（仅该方法）。
     */
    public int[] getEndpointConfig(String method, String endpoint) {
        String scoped = (method == null ? "" : method.toUpperCase() + " ") + endpoint;
        String configured = findValue(endpoints, scoped);
        if (configured == null) {
            configured = findValue(DEFAULT_ENDPOINTS, scoped);
        }
        if (configured == null) {
            configured = findValue(endpoints, endpoint);
        }
        if (configured == null) {
            configured = findValue(DEFAULT_ENDPOINTS, endpoint);
        }
        return configured != null ? parseConfig(configured) : new int[]{defaultLimit, defaultWindow};
    }

    /**
     * 返回命中的规则模式（如 "POST /api/ai/write/**"），未命中返回 null。
     * 限流 key 应使用该模式而非原始 URI（SEC-P2-1）：
     * 否则 "/api/articles/123/like" 与 "/api/articles/456/like" 会被当作不同端点各自计数，
     * 变量路径既稀释了限流粒度，爬虫还能靠随机路径撑爆 Redis key 空间。
     */
    public String resolveRulePattern(String method, String endpoint) {
        String scoped = (method == null ? "" : method.toUpperCase() + " ") + endpoint;
        String pattern = findPattern(endpoints, scoped);
        if (pattern == null) {
            pattern = findPattern(DEFAULT_ENDPOINTS, scoped);
        }
        if (pattern == null) {
            pattern = findPattern(endpoints, endpoint);
        }
        if (pattern == null) {
            pattern = findPattern(DEFAULT_ENDPOINTS, endpoint);
        }
        return pattern;
    }

    private String findValue(Map<String, String> configs, String endpoint) {
        return configs.entrySet().stream()
                .filter(entry -> endpoint.equals(entry.getKey()) || PATH_MATCHER.match(entry.getKey(), endpoint))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    private String findPattern(Map<String, String> configs, String endpoint) {
        return configs.entrySet().stream()
                .filter(entry -> endpoint.equals(entry.getKey()) || PATH_MATCHER.match(entry.getKey(), endpoint))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private int[] parseConfig(String config) {
        if (config == null) {
            return new int[]{defaultLimit, defaultWindow};
        }
        String[] parts = config.split(",");
        if (parts.length != 2) {
            return new int[]{defaultLimit, defaultWindow};
        }
        try {
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (NumberFormatException e) {
            return new int[]{defaultLimit, defaultWindow};
        }
    }
}
