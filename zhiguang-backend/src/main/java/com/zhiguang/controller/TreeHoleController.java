package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.treehole.TreeHolePostDTO;
import com.zhiguang.dto.treehole.TreeHoleReactionDTO;
import com.zhiguang.dto.treehole.TreeHoleReplyDTO;
import com.zhiguang.dto.treehole.TreeHoleReportDTO;
import com.zhiguang.service.AnonymousIdentityService;
import com.zhiguang.service.TreeHoleIdentityService;
import com.zhiguang.service.TreeHoleService;
import com.zhiguang.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tree-hole")
public class TreeHoleController {

    private static final String ANON_HEADER = "X-Anonymous-Id";
    private static final String ANON_COOKIE = "tree_hole_anon_id";
    /** 匿名身份无效的业务码，前端据此重新签发身份并重试 */
    public static final int CODE_IDENTITY_INVALID = 4401;

    /** 身份签发接口按 IP 限频：防脚本批量领身份轮换刷帖 */
    private static final int IDENTITY_ISSUE_LIMIT_PER_HOUR = 10;

    @Autowired
    private TreeHoleService treeHoleService;

    @Autowired
    private TreeHoleIdentityService identityService;

    @Autowired
    private AnonymousIdentityService anonymousIdentityService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 签发/续期匿名身份。
     * body 可携带当前 token：验签通过则同一 anonId 续期（属主关系不变），否则签发新身份。
     * 返回 token 与昵称/徽标（昵称由服务端按身份哈希派生，前端不再本地模拟）。
     */
    @PostMapping("/identity")
    public ApiResponse<?> issueIdentity(@RequestBody(required = false) Map<String, String> body,
                                        HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        String key = "treehole:identity:ip:" + ip;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofHours(1));
            }
            if (count != null && count > IDENTITY_ISSUE_LIMIT_PER_HOUR) {
                return ApiResponse.error(429, "身份获取过于频繁，请稍后再试");
            }
        } catch (Exception ignore) {
            // Redis 故障时不阻断身份签发（写操作仍有按身份与按 IP 的双重限流）
        }

        String current = body != null ? body.get("token") : null;
        // 兼容旧客户端从 header/cookie 带旧 id 的升级请求
        if (current == null || current.isBlank()) {
            current = extractRawIdentity(request);
        }
        TreeHoleIdentityService.IssuedIdentity issued = identityService.refreshOrIssue(current);
        Map<String, String> identity = anonymousIdentityService.generateIdentity(identityService.hash(issued.anonId()));

        Map<String, Object> data = new HashMap<>();
        data.put("token", issued.token());
        data.put("expiresAt", issued.expiresAt());
        data.put("displayName", identity.get("displayName"));
        data.put("emoji", identity.get("emoji"));
        data.put("color", identity.get("color"));
        return ApiResponse.success(data);
    }

    /** 写操作身份：无效即拒绝（不再接受"缺失则随机生成"——那等于允许任意伪造） */
    private String requireIdentityHash(HttpServletRequest request) {
        TreeHoleIdentityService.ResolvedIdentity resolved =
                identityService.resolve(extractRawIdentity(request));
        if (!resolved.valid()) {
            return null;
        }
        return identityService.hash(resolved.anonId());
    }

    /** 读操作身份：无效时降级为一次性随机身份（仅影响个性化标记，不涉及属主） */
    private String optionalIdentityHash(HttpServletRequest request) {
        TreeHoleIdentityService.ResolvedIdentity resolved =
                identityService.resolve(extractRawIdentity(request));
        String anonId = resolved.valid() ? resolved.anonId() : UUID.randomUUID().toString();
        return identityService.hash(anonId);
    }

    private String extractRawIdentity(HttpServletRequest request) {
        String id = request.getHeader(ANON_HEADER);
        if (id == null || id.isBlank()) {
            id = getCookieValue(request, ANON_COOKIE);
        }
        return (id == null || id.isBlank()) ? null : id.trim();
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @GetMapping("/posts")
    public ApiResponse<?> listPosts(
            @RequestParam(required = false, defaultValue = "all") String emotion,
            @RequestParam(required = false, defaultValue = "recommend") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            HttpServletRequest request) {
        String anonymousHash = optionalIdentityHash(request);
        return treeHoleService.listPosts(emotion, sort, page, size, anonymousHash);
    }

    @PostMapping("/posts")
    public ApiResponse<?> createPost(@Valid @RequestBody TreeHolePostDTO dto, HttpServletRequest request) {
        String hash = requireIdentityHash(request);
        if (hash == null) {
            return ApiResponse.error(CODE_IDENTITY_INVALID, "匿名身份无效或已过期，请刷新页面后重试");
        }
        return treeHoleService.createPost(hash, IpUtil.getClientIp(request), dto);
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<?> getPostDetail(@PathVariable Long id, HttpServletRequest request) {
        return treeHoleService.getPostDetail(id, optionalIdentityHash(request));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<?> deletePost(@PathVariable Long id, HttpServletRequest request) {
        String hash = requireIdentityHash(request);
        if (hash == null) {
            return ApiResponse.error(CODE_IDENTITY_INVALID, "匿名身份无效或已过期，请刷新页面后重试");
        }
        return treeHoleService.deletePost(id, hash);
    }

    @PostMapping("/replies")
    public ApiResponse<?> createReply(@Valid @RequestBody TreeHoleReplyDTO dto, HttpServletRequest request) {
        String hash = requireIdentityHash(request);
        if (hash == null) {
            return ApiResponse.error(CODE_IDENTITY_INVALID, "匿名身份无效或已过期，请刷新页面后重试");
        }
        return treeHoleService.createReply(hash, IpUtil.getClientIp(request), dto);
    }

    @DeleteMapping("/replies/{id}")
    public ApiResponse<?> deleteReply(@PathVariable Long id, HttpServletRequest request) {
        String hash = requireIdentityHash(request);
        if (hash == null) {
            return ApiResponse.error(CODE_IDENTITY_INVALID, "匿名身份无效或已过期，请刷新页面后重试");
        }
        return treeHoleService.deleteReply(id, hash);
    }

    @PostMapping("/reactions")
    public ApiResponse<?> toggleReaction(@Valid @RequestBody TreeHoleReactionDTO dto, HttpServletRequest request) {
        String hash = requireIdentityHash(request);
        if (hash == null) {
            return ApiResponse.error(CODE_IDENTITY_INVALID, "匿名身份无效或已过期，请刷新页面后重试");
        }
        return treeHoleService.toggleReaction(hash, IpUtil.getClientIp(request), dto);
    }

    @PostMapping("/reports")
    public ApiResponse<?> report(@Valid @RequestBody TreeHoleReportDTO dto, HttpServletRequest request) {
        String hash = requireIdentityHash(request);
        if (hash == null) {
            return ApiResponse.error(CODE_IDENTITY_INVALID, "匿名身份无效或已过期，请刷新页面后重试");
        }
        return treeHoleService.report(hash, IpUtil.getClientIp(request), dto);
    }

    @GetMapping("/recommend")
    public ApiResponse<?> recommend(
            @RequestParam(required = false, defaultValue = "10") int size,
            HttpServletRequest request) {
        return treeHoleService.recommend(optionalIdentityHash(request), size);
    }

    @GetMapping("/emotion-stats")
    public ApiResponse<?> emotionStats() {
        return treeHoleService.emotionStats();
    }

    @GetMapping("/emotion-trends")
    public ApiResponse<?> emotionTrends(
            @RequestParam(required = false, defaultValue = "7") int days) {
        return treeHoleService.emotionTrends(Math.min(days, 30));
    }

    @GetMapping("/hot-topics")
    public ApiResponse<?> hotTopics(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return treeHoleService.hotTopics(limit);
    }
}
