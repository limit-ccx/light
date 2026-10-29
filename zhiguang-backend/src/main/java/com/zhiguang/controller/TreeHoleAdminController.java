package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.treehole.AdminPostActionDTO;
import com.zhiguang.entity.TreeHolePost;
import com.zhiguang.entity.TreeHoleReply;
import com.zhiguang.entity.TreeHoleReport;
import com.zhiguang.entity.User;
import com.zhiguang.repository.TreeHolePostRepository;
import com.zhiguang.repository.TreeHoleReplyRepository;
import com.zhiguang.repository.TreeHoleReportRepository;
import com.zhiguang.service.AdminLogService;
import com.zhiguang.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tree-hole")
public class TreeHoleAdminController {

    @Autowired
    private TreeHolePostRepository postRepository;

    @Autowired
    private TreeHoleReplyRepository replyRepository;

    @Autowired
    private TreeHoleReportRepository reportRepository;

    @Autowired
    private AdminLogService adminLogService;

    /** 按状态查询帖子 */
    @GetMapping("/posts")
    public ApiResponse<?> listPostsByStatus(
            @RequestParam(required = false, defaultValue = "0") Integer status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TreeHolePost> result = postRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        List<Map<String, Object>> posts = result.getContent().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            String content = p.getContent();
            map.put("content", content.length() > 100 ? content.substring(0, 100) + "..." : content);
            map.put("fullContent", p.getContent());
            map.put("emotionTag", p.getEmotionTag());
            map.put("status", p.getStatus());
            map.put("likeCount", p.getLikeCount());
            map.put("empathyCount", p.getEmpathyCount());
            map.put("replyCount", p.getReplyCount());
            map.put("reportCount", p.getReportCount());
            map.put("viewCount", p.getViewCount());
            map.put("score", p.getScore());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).toList();
        return ApiResponse.success(Map.of(
                "posts", posts,
                "total", result.getTotalElements(),
                "pages", result.getTotalPages(),
                "current", result.getNumber()
        ));
    }

    /** 审核通过 */
    @PutMapping("/posts/{id}/approve")
    public ApiResponse<?> approvePost(@PathVariable Long id,
                                      @AuthenticationPrincipal User admin,
                                      HttpServletRequest request) {
        TreeHolePost post = postRepository.findById(id).orElse(null);
        if (post == null) return ApiResponse.error(404, "帖子不存在");
        post.setStatus(1);
        postRepository.save(post);

        adminLogService.log(admin.getId(), "TREEHOLE_APPROVE_POST", "TREEHOLE_POST", id,
                "审核通过帖子: " + post.getContent().substring(0, Math.min(50, post.getContent().length())),
                IpUtil.getClientIp(request));

        return ApiResponse.success("已通过审核");
    }

    /** 驳回帖子 */
    @PutMapping("/posts/{id}/reject")
    public ApiResponse<?> rejectPost(@PathVariable Long id,
                                     @RequestBody(required = false) AdminPostActionDTO dto,
                                     @AuthenticationPrincipal User admin,
                                     HttpServletRequest request) {
        TreeHolePost post = postRepository.findById(id).orElse(null);
        if (post == null) return ApiResponse.error(404, "帖子不存在");
        post.setStatus(2);
        postRepository.save(post);

        String reason = dto != null ? dto.getReason() : "";
        adminLogService.log(admin.getId(), "TREEHOLE_REJECT_POST", "TREEHOLE_POST", id,
                "驳回帖子: " + post.getContent().substring(0, Math.min(50, post.getContent().length())) + ", 原因: " + reason,
                IpUtil.getClientIp(request));

        return ApiResponse.success("已驳回");
    }

    /** 管理员删除帖子 */
    @DeleteMapping("/posts/{id}")
    public ApiResponse<?> deletePost(@PathVariable Long id,
                                     @AuthenticationPrincipal User admin,
                                     HttpServletRequest request) {
        TreeHolePost post = postRepository.findById(id).orElse(null);
        if (post == null) return ApiResponse.error(404, "帖子不存在");
        post.setStatus(3);
        postRepository.save(post);

        adminLogService.log(admin.getId(), "TREEHOLE_DELETE_POST", "TREEHOLE_POST", id,
                "删除帖子: " + post.getContent().substring(0, Math.min(50, post.getContent().length())),
                IpUtil.getClientIp(request));

        return ApiResponse.success("已删除");
    }

    /** 管理员删除回复 */
    @DeleteMapping("/replies/{id}")
    public ApiResponse<?> deleteReply(@PathVariable Long id,
                                      @AuthenticationPrincipal User admin,
                                      HttpServletRequest request) {
        TreeHoleReply reply = replyRepository.findById(id).orElse(null);
        if (reply == null) return ApiResponse.error(404, "回复不存在");
        reply.setStatus(3);
        replyRepository.save(reply);
        // Update parent post's reply count and score
        postRepository.decrementReplyCount(reply.getPostId());
        postRepository.findById(reply.getPostId()).ifPresent(post -> {
            double hours = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now()) + 2;
            double timeDecay = 1.0 / Math.pow(hours, 0.8);
            double engagement = post.getLikeCount() * 2 + post.getEmpathyCount() * 3 + post.getReplyCount() * 4 + post.getViewCount() * 0.5;
            post.setScore(engagement * timeDecay);
            postRepository.save(post);
        });

        adminLogService.log(admin.getId(), "TREEHOLE_DELETE_REPLY", "TREEHOLE_REPLY", id,
                "删除回复: " + reply.getContent().substring(0, Math.min(50, reply.getContent().length())),
                IpUtil.getClientIp(request));

        return ApiResponse.success("已删除");
    }

    /** 举报列表 */
    @GetMapping("/reports")
    public ApiResponse<?> listReports(
            @RequestParam(required = false, defaultValue = "0") Integer status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TreeHoleReport> result = reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        List<Map<String, Object>> reports = result.getContent().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("targetType", r.getTargetType());
            map.put("targetId", r.getTargetId());
            map.put("reason", r.getReason());
            map.put("status", r.getStatus());
            map.put("createdAt", r.getCreatedAt());
            // Fetch reported content
            if ("POST".equals(r.getTargetType())) {
                postRepository.findById(r.getTargetId()).ifPresent(p -> {
                    String content = p.getContent();
                    map.put("targetContent", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    map.put("targetEmotionTag", p.getEmotionTag());
                    map.put("targetStatus", p.getStatus());
                });
            } else {
                replyRepository.findById(r.getTargetId()).ifPresent(reply -> {
                    String content = reply.getContent();
                    map.put("targetContent", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    map.put("targetStatus", reply.getStatus());
                });
            }
            return map;
        }).toList();
        return ApiResponse.success(Map.of(
                "reports", reports,
                "total", result.getTotalElements(),
                "pages", result.getTotalPages(),
                "current", result.getNumber()
        ));
    }

    /** 处理举报（隐藏被举报内容） */
    @PutMapping("/reports/{id}/resolve")
    public ApiResponse<?> resolveReport(@PathVariable Long id,
                                        @AuthenticationPrincipal User admin,
                                        HttpServletRequest request) {
        TreeHoleReport report = reportRepository.findById(id).orElse(null);
        if (report == null) return ApiResponse.error(404, "举报不存在");
        report.setStatus(1); // resolved
        reportRepository.save(report);
        // Hide the reported content
        if ("POST".equals(report.getTargetType())) {
            postRepository.findById(report.getTargetId()).ifPresent(post -> {
                post.setStatus(2); // rejected/hidden
                postRepository.save(post);
            });
        } else {
            replyRepository.findById(report.getTargetId()).ifPresent(reply -> {
                reply.setStatus(2);
                replyRepository.save(reply);
            });
        }

        adminLogService.log(admin.getId(), "TREEHOLE_RESOLVE_REPORT", "TREEHOLE_REPORT", id,
                "处理举报: " + report.getReason() + ", 目标类型: " + report.getTargetType() + ", 目标ID: " + report.getTargetId(),
                IpUtil.getClientIp(request));

        return ApiResponse.success("已处理");
    }

    /** 忽略举报 */
    @PutMapping("/reports/{id}/dismiss")
    public ApiResponse<?> dismissReport(@PathVariable Long id,
                                        @AuthenticationPrincipal User admin,
                                        HttpServletRequest request) {
        TreeHoleReport report = reportRepository.findById(id).orElse(null);
        if (report == null) return ApiResponse.error(404, "举报不存在");
        report.setStatus(2); // ignored
        reportRepository.save(report);

        adminLogService.log(admin.getId(), "TREEHOLE_DISMISS_REPORT", "TREEHOLE_REPORT", id,
                "忽略举报: " + report.getReason(),
                IpUtil.getClientIp(request));

        return ApiResponse.success("已忽略");
    }

    /** 树洞统计数据 */
    @GetMapping("/stats")
    public ApiResponse<?> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Count posts by status
        long totalPosts = postRepository.count();
        stats.put("totalPosts", totalPosts);

        // Pending posts (status=0)
        long pendingPosts = postRepository.findByStatusOrderByCreatedAtDesc(0, PageRequest.of(0, 1)).getTotalElements();
        stats.put("pendingPosts", pendingPosts);

        // Active posts (status=1)
        long activePosts = postRepository.findByStatusOrderByCreatedAtDesc(1, PageRequest.of(0, 1)).getTotalElements();
        stats.put("activePosts", activePosts);

        // Rejected posts (status=2)
        long rejectedPosts = postRepository.findByStatusOrderByCreatedAtDesc(2, PageRequest.of(0, 1)).getTotalElements();
        stats.put("rejectedPosts", rejectedPosts);

        // Today's posts
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayPosts = countPostsSince(todayStart);
        stats.put("todayPosts", todayPosts);

        // Reports
        long totalReports = reportRepository.count();
        stats.put("totalReports", totalReports);

        long unresolvedReports = reportRepository.findByStatusOrderByCreatedAtDesc(0, PageRequest.of(0, 1)).getTotalElements();
        stats.put("unresolvedReports", unresolvedReports);

        // Emotion distribution
        Map<String, Long> emotionDist = postRepository.countByStatusGroupByEmotionTag(1).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));
        stats.put("emotionDistribution", emotionDist);

        return ApiResponse.success(stats);
    }

    private long countPostsSince(LocalDateTime since) {
        return postRepository.countByStatusAndCreatedAtAfter(1, since);
    }
}
