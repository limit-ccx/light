package com.zhiguang.service.agent.admin.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.entity.User;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.UserRepository;
import com.zhiguang.service.agent.admin.AdminAgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 用户信息查询（邮箱脱敏，§6；不返回密码哈希/securityVersion）。 */
@Component
@RequiredArgsConstructor
public class QueryUserTool implements AdminAgentTool {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "query_user";
    }

    @Override
    public String description() {
        return "按用户名精确查询或按用户 ID 查询用户公开信息（邮箱脱敏）。封禁等处置前先查询确认用户存在。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "username", Map.of("type", "string", "description", "用户名（精确）"),
                        "userId", Map.of("type", "integer", "description", "用户 ID")),
                "required", List.of());
    }

    @Override
    public String execute(Long adminId, Map<String, Object> args) throws Exception {
        Optional<User> userOpt;
        if (args.get("userId") != null) {
            long id = parseLong(args.get("userId"), -1);
            userOpt = id > 0 ? userRepository.findById(id) : Optional.empty();
        } else if (args.get("username") != null) {
            userOpt = userRepository.findByUsername(String.valueOf(args.get("username")).trim());
        } else {
            return "{\"error\":\"需要 username 或 userId\"}";
        }
        return userOpt.map(user -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", user.getId());
            m.put("username", user.getUsername());
            m.put("email", maskEmail(user.getEmail()));
            m.put("status", user.getStatus());
            m.put("role", user.getRole());
            m.put("createdAt", String.valueOf(user.getCreatedAt()));
            try {
                m.put("publishedArticles", articleRepository.countByAuthorIdAndStatus(user.getId(), 2));
            } catch (Exception ignored) {
            }
            return toJson(m);
        }).orElse("{\"error\":\"user not found\"}");
    }

    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String masked = local.length() <= 1 ? local + "***"
                : local.charAt(0) + "***" + local.charAt(local.length() - 1);
        return masked + email.substring(at);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\":\"serialize failed\"}";
        }
    }

    private long parseLong(Object value, long defaultValue) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
