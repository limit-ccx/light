package com.zhiguang.service;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.treehole.TreeHolePostDTO;
import com.zhiguang.dto.treehole.TreeHoleReactionDTO;
import com.zhiguang.dto.treehole.TreeHoleReplyDTO;
import com.zhiguang.dto.treehole.TreeHoleReportDTO;
import com.zhiguang.entity.TreeHolePost;
import com.zhiguang.entity.TreeHoleReaction;
import com.zhiguang.entity.TreeHoleReply;
import com.zhiguang.entity.TreeHoleReport;
import com.zhiguang.repository.TreeHolePostRepository;
import com.zhiguang.repository.TreeHoleReactionRepository;
import com.zhiguang.repository.TreeHoleReplyRepository;
import com.zhiguang.repository.TreeHoleReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TreeHoleService {

    private static final int POST_RATE_LIMIT_HOUR = 5;
    private static final int REPLY_RATE_LIMIT_HOUR = 10;
    private static final int REACTION_RATE_LIMIT_HOUR = 30;
    private static final int REPORT_RATE_LIMIT_DAY = 20;
    private static final int REPORT_HIDE_THRESHOLD = 3;

    @Autowired
    private TreeHolePostRepository postRepository;

    @Autowired
    private TreeHoleReplyRepository replyRepository;

    @Autowired
    private TreeHoleReactionRepository reactionRepository;

    @Autowired
    private TreeHoleReportRepository reportRepository;

    @Autowired
    private TreeHoleModerationService moderationService;

    @Autowired
    private TreeHoleAiService treeHoleAiService;

    @Autowired
    private AnonymousIdentityService anonymousIdentityService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ===================== 帖子 =====================

    @Transactional
    public ApiResponse<?> createPost(String anonymousHash, String clientIp, TreeHolePostDTO dto) {
        if (exceedsIpWriteLimit(clientIp, "post", 15)) {
            return ApiResponse.error(429, "发布过于频繁，请稍后再试");
        }
        if (exceedsPostRateLimit(anonymousHash)) {
            return ApiResponse.error(429, "发布过于频繁，请稍后再试");
        }

        var moderation = moderationService.moderate(dto.getContent());
        if (!moderation.approved() && !moderation.pending()) {
            return ApiResponse.error(400, moderation.reason());
        }

        TreeHolePost post = new TreeHolePost();
        post.setAnonymousHash(anonymousHash);
        post.setContent(dto.getContent().trim());
        post.setEmotionTag(normalizeEmotion(dto.getEmotionTag()));
        post.setStatus(moderation.pending() ? 0 : 1);
        post.setScore(calculateInitialScore(post));
        postRepository.save(post);

        if (treeHoleAiService.shouldGenerateResponse(post.getEmotionTag())) {
            treeHoleAiService.generateWarmResponseAsync(post.getId(), post.getContent(), post.getEmotionTag());
        }

        String msg = moderation.pending() ? "内容已提交，审核通过后将展示" : "发布成功";
        return ApiResponse.success(msg, Map.of("id", post.getId(), "status", post.getStatus()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<?> listPosts(String emotionTag, String sort, int page, int size, String currentHash) {
        Pageable pageable = PageRequest.of(page, size);
        boolean latest = "latest".equalsIgnoreCase(sort);
        Page<TreeHolePost> result;
        if (emotionTag != null && !emotionTag.isEmpty() && !"all".equalsIgnoreCase(emotionTag)) {
            result = latest
                    ? postRepository.findByStatusAndEmotionTagOrderByCreatedAtDesc(1, emotionTag, pageable)
                    : postRepository.findByStatusAndEmotionTagOrderByScoreDescCreatedAtDesc(1, emotionTag, pageable);
        } else {
            result = latest
                    ? postRepository.findByStatusOrderByCreatedAtDesc(1, pageable)
                    : postRepository.findByStatusOrderByScoreDescCreatedAtDesc(1, pageable);
        }
        List<TreeHolePost> posts = result.getContent();
        Set<Long> postIds = posts.stream().map(TreeHolePost::getId).collect(Collectors.toSet());
        Map<Long, Map<String, Boolean>> reactionMap = getUserReactionsBatch(currentHash, postIds);
        return ApiResponse.success(Map.of(
                "posts", posts.stream().map(p -> maskPost(p, currentHash, reactionMap.get(p.getId()))).toList(),
                "total", result.getTotalElements(),
                "pages", result.getTotalPages(),
                "current", result.getNumber()
        ));
    }

    @Transactional
    public ApiResponse<?> getPostDetail(Long id, String anonymousHash) {
        TreeHolePost post = postRepository.findById(id).orElse(null);
        if (post == null || post.getStatus() != 1) {
            return ApiResponse.error(404, "内容不存在");
        }
        postRepository.incrementViewCount(id);
        return ApiResponse.success(Map.of(
                "post", maskPost(post, anonymousHash),
                "replies", replyRepository.findByPostIdAndStatusOrderByCreatedAtAsc(id, 1)
                        .stream().map(r -> maskReply(r, anonymousHash)).toList(),
                "userReactions", getUserReactions(anonymousHash, id)
        ));
    }

    @Transactional
    public ApiResponse<?> deletePost(Long id, String anonymousHash) {
        TreeHolePost post = postRepository.findById(id).orElse(null);
        if (post == null) return ApiResponse.error(404, "内容不存在");
        if (!post.getAnonymousHash().equals(anonymousHash)) {
            return ApiResponse.error(403, "只能删除自己发布的内容");
        }
        post.setStatus(3);
        postRepository.save(post);
        return ApiResponse.success("已删除");
    }

    // ===================== 回复 =====================

    @Transactional
    public ApiResponse<?> createReply(String anonymousHash, String clientIp, TreeHoleReplyDTO dto) {
        if (exceedsIpWriteLimit(clientIp, "reply", 30)) {
            return ApiResponse.error(429, "回复过于频繁，请稍后再试");
        }
        if (exceedsReplyRateLimit(anonymousHash)) {
            return ApiResponse.error(429, "回复过于频繁，请稍后再试");
        }
        TreeHolePost post = postRepository.findById(dto.getPostId()).orElse(null);
        if (post == null || post.getStatus() != 1) {
            return ApiResponse.error(404, "内容不存在");
        }
        var moderation = moderationService.moderate(dto.getContent());
        if (!moderation.approved() && !moderation.pending()) {
            return ApiResponse.error(400, moderation.reason());
        }

        TreeHoleReply reply = new TreeHoleReply();
        reply.setPostId(dto.getPostId());
        reply.setAnonymousHash(anonymousHash);
        reply.setContent(dto.getContent().trim());
        reply.setStatus(moderation.pending() ? 0 : 1);
        replyRepository.save(reply);
        postRepository.incrementReplyCount(dto.getPostId());
        refreshPostScore(post);

        String msg = moderation.pending() ? "回复已提交，审核通过后将展示" : "回复成功";
        return ApiResponse.success(msg, Map.of("id", reply.getId()));
    }

    @Transactional
    public ApiResponse<?> deleteReply(Long id, String anonymousHash) {
        TreeHoleReply reply = replyRepository.findById(id).orElse(null);
        if (reply == null) return ApiResponse.error(404, "回复不存在");
        if (!reply.getAnonymousHash().equals(anonymousHash)) {
            return ApiResponse.error(403, "只能删除自己发布的回复");
        }
        reply.setStatus(3);
        replyRepository.save(reply);
        postRepository.decrementReplyCount(reply.getPostId());
        postRepository.findById(reply.getPostId()).ifPresent(this::refreshPostScore);
        return ApiResponse.success("已删除");
    }

    // ===================== 反应 =====================

    @Transactional
    public ApiResponse<?> toggleReaction(String anonymousHash, String clientIp, TreeHoleReactionDTO dto) {
        if (exceedsIpWriteLimit(clientIp, "reaction", 60)) {
            return ApiResponse.error(429, "操作过于频繁，请稍后再试");
        }
        if (exceedsReactionRateLimit(anonymousHash)) {
            return ApiResponse.error(429, "操作过于频繁，请稍后再试");
        }
        String targetType = dto.getTargetType().toUpperCase();
        String reactionType = dto.getReactionType().toUpperCase();
        if (!Arrays.asList("POST", "REPLY").contains(targetType) ||
            !Arrays.asList("LIKE", "EMPATHY").contains(reactionType)) {
            return ApiResponse.error(400, "参数错误");
        }

        boolean exists = reactionRepository.existsByAnonymousHashAndTargetTypeAndTargetIdAndReactionType(
                anonymousHash, targetType, dto.getTargetId(), reactionType);

        if (exists) {
            reactionRepository.deleteReaction(anonymousHash, targetType, dto.getTargetId(), reactionType);
            updateReactionCount(targetType, dto.getTargetId(), reactionType, -1);
            return ApiResponse.success("已取消", Map.of("active", false));
        }

        TreeHoleReaction reaction = new TreeHoleReaction();
        reaction.setAnonymousHash(anonymousHash);
        reaction.setTargetType(targetType);
        reaction.setTargetId(dto.getTargetId());
        reaction.setReactionType(reactionType);
        reactionRepository.save(reaction);
        updateReactionCount(targetType, dto.getTargetId(), reactionType, 1);

        if ("POST".equals(targetType)) {
            postRepository.findById(dto.getTargetId()).ifPresent(this::refreshPostScore);
        }
        return ApiResponse.success("已表达", Map.of("active", true));
    }

    private void updateReactionCount(String targetType, Long targetId, String reactionType, int delta) {
        if ("POST".equals(targetType)) {
            TreeHolePost post = postRepository.findById(targetId).orElse(null);
            if (post == null) return;
            if ("LIKE".equals(reactionType)) {
                post.setLikeCount(Math.max(0, post.getLikeCount() + delta));
            } else {
                post.setEmpathyCount(Math.max(0, post.getEmpathyCount() + delta));
            }
            postRepository.save(post);
        } else {
            TreeHoleReply reply = replyRepository.findById(targetId).orElse(null);
            if (reply == null) return;
            if ("LIKE".equals(reactionType)) {
                reply.setLikeCount(Math.max(0, reply.getLikeCount() + delta));
            } else {
                reply.setEmpathyCount(Math.max(0, reply.getEmpathyCount() + delta));
            }
            replyRepository.save(reply);
        }
    }

    // ===================== 举报 =====================

    @Transactional
    public ApiResponse<?> report(String anonymousHash, String clientIp, TreeHoleReportDTO dto) {
        if (exceedsIpWriteLimit(clientIp, "report", 10)) {
            return ApiResponse.error(429, "举报过于频繁，请稍后再试");
        }
        if (exceedsReportRateLimit(anonymousHash)) {
            return ApiResponse.error(429, "举报过于频繁，请稍后再试");
        }
        String targetType = dto.getTargetType().toUpperCase();
        if (!Arrays.asList("POST", "REPLY").contains(targetType)) {
            return ApiResponse.error(400, "参数错误");
        }
        Optional<TreeHoleReport> existing = reportRepository.findByAnonymousHashAndTargetTypeAndTargetId(
                anonymousHash, targetType, dto.getTargetId());
        if (existing.isPresent()) {
            return ApiResponse.error(400, "您已经举报过该内容");
        }

        TreeHoleReport report = new TreeHoleReport();
        report.setAnonymousHash(anonymousHash);
        report.setTargetType(targetType);
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        reportRepository.save(report);

        if ("POST".equals(targetType)) {
            postRepository.incrementReportCount(dto.getTargetId());
            postRepository.findById(dto.getTargetId()).ifPresent(post -> {
                if (post.getReportCount() >= REPORT_HIDE_THRESHOLD) {
                    post.setStatus(0);
                    postRepository.save(post);
                }
            });
        }
        return ApiResponse.success("举报已提交，我们会尽快处理");
    }

    // ===================== 推荐 =====================

    @Transactional(readOnly = true)
    public ApiResponse<?> recommend(String anonymousHash, int size) {
        List<TreeHolePost> active = postRepository.findByStatusOrderByScoreDescCreatedAtDesc(1, PageRequest.of(0, 100)).getContent();
        if (active.isEmpty()) return ApiResponse.success(Collections.emptyList());

        // 根据最近互动过的情绪标签加权
        Set<String> preferredEmotions = getPreferredEmotions(anonymousHash);
        List<TreeHolePost> scored = active.stream().map(p -> {
            double s = p.getScore();
            if (preferredEmotions.contains(p.getEmotionTag())) s *= 1.3;
            // 随机扰动，避免重复
            s += Math.random() * 5;
            return new AbstractMap.SimpleEntry<>(p, s);
        }).sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
          .limit(size)
          .map(AbstractMap.SimpleEntry::getKey)
          .toList();
        return ApiResponse.success(scored.stream().map(p -> maskPost(p, anonymousHash)).toList());
    }

    @Transactional(readOnly = true)
    public ApiResponse<?> emotionStats() {
        Map<String, Long> stats = postRepository.countByStatusGroupByEmotionTag(1).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        Long::sum));
        return ApiResponse.success(stats);
    }

    @Transactional(readOnly = true)
    public ApiResponse<?> emotionTrends(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = postRepository.getEmotionTrends(since);

        // Collect unique dates and emotions
        java.util.Set<String> dateSet = new java.util.TreeSet<>();
        java.util.Set<String> emotionSet = new java.util.LinkedHashSet<>();
        for (Object[] row : rows) {
            emotionSet.add((String) row[0]);
            dateSet.add(String.valueOf(row[2]));
        }

        List<String> dates = new java.util.ArrayList<>(dateSet);
        Map<String, List<Long>> emotions = new java.util.LinkedHashMap<>();
        for (String emotion : emotionSet) {
            List<Long> counts = new java.util.ArrayList<>();
            for (int i = 0; i < dates.size(); i++) counts.add(0L);
            emotions.put(emotion, counts);
        }

        // Fill in data
        for (Object[] row : rows) {
            String emotion = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            String date = String.valueOf(row[2]);
            int dateIdx = dates.indexOf(date);
            if (dateIdx >= 0 && emotions.containsKey(emotion)) {
                emotions.get(emotion).set(dateIdx, count);
            }
        }

        return ApiResponse.success(Map.of("dates", dates, "emotions", emotions));
    }

    @Transactional(readOnly = true)
    public ApiResponse<?> hotTopics(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, Math.min(limit, 20));
        List<TreeHolePost> posts = postRepository.findByStatusOrderByScoreDescCreatedAtDesc(1, pageable).getContent();

        List<Map<String, Object>> topics = posts.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            String content = p.getContent();
            map.put("content", content.length() > 50 ? content.substring(0, 50) + "..." : content);
            map.put("emotionTag", p.getEmotionTag());
            map.put("likeCount", p.getLikeCount());
            map.put("empathyCount", p.getEmpathyCount());
            map.put("replyCount", p.getReplyCount());
            map.put("score", p.getScore());
            return map;
        }).toList();

        return ApiResponse.success(topics);
    }

    private Set<String> getPreferredEmotions(String anonymousHash) {
        List<TreeHoleReaction> reactions = reactionRepository.findByAnonymousHash(anonymousHash);
        Set<Long> postIds = reactions.stream()
                .filter(r -> "POST".equals(r.getTargetType()))
                .map(TreeHoleReaction::getTargetId)
                .collect(java.util.stream.Collectors.toSet());
        return postRepository.findAllById(postIds).stream()
                .map(TreeHolePost::getEmotionTag)
                .collect(java.util.stream.Collectors.toSet());
    }

    // ===================== 工具方法 =====================

    private Map<String, Object> maskPost(TreeHolePost post, String currentHash) {
        return maskPost(post, currentHash, null);
    }

    private Map<String, Object> maskPost(TreeHolePost post, String currentHash, Map<String, Boolean> reactions) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("content", post.getContent());
        map.put("emotionTag", post.getEmotionTag());
        map.put("likeCount", post.getLikeCount());
        map.put("empathyCount", post.getEmpathyCount());
        map.put("replyCount", post.getReplyCount());
        map.put("viewCount", post.getViewCount());
        map.put("createdAt", post.getCreatedAt());
        map.put("isOwner", currentHash != null && currentHash.equals(post.getAnonymousHash()));
        map.put("userReacted", reactions != null ? reactions : Map.of("like", false, "empathy", false));
        map.put("anonIdentity", anonymousIdentityService.generateIdentity(post.getAnonymousHash()));
        return map;
    }

    private Map<String, Object> maskReply(TreeHoleReply reply, String currentHash) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", reply.getId());
        map.put("postId", reply.getPostId());
        map.put("content", reply.getContent());
        map.put("likeCount", reply.getLikeCount());
        map.put("empathyCount", reply.getEmpathyCount());
        map.put("createdAt", reply.getCreatedAt());
        map.put("isOwner", currentHash != null && currentHash.equals(reply.getAnonymousHash()));
        map.put("isAiGenerated", reply.getIsAiGenerated() != null && reply.getIsAiGenerated());
        map.put("anonIdentity", anonymousIdentityService.generateIdentity(reply.getAnonymousHash()));
        return map;
    }

    private Map<String, Object> getUserReactions(String anonymousHash, Long postId) {
        Map<String, Boolean> batch = getUserReactionsBatch(anonymousHash, Set.of(postId)).get(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("postLike", batch != null && Boolean.TRUE.equals(batch.get("like")));
        result.put("postEmpathy", batch != null && Boolean.TRUE.equals(batch.get("empathy")));
        return result;
    }

    private Map<Long, Map<String, Boolean>> getUserReactionsBatch(String anonymousHash, Set<Long> postIds) {
        Map<Long, Map<String, Boolean>> map = new HashMap<>();
        if (anonymousHash == null || anonymousHash.isBlank() || postIds == null || postIds.isEmpty()) {
            return map;
        }
        for (TreeHoleReaction r : reactionRepository.findByAnonymousHashAndTargetTypeAndTargetIdIn(
                anonymousHash, "POST", postIds.stream().toList())) {
            map.computeIfAbsent(r.getTargetId(), k -> new HashMap<>())
               .put(r.getReactionType().toLowerCase(), true);
        }
        return map;
    }

    private double calculateInitialScore(TreeHolePost post) {
        return post.getLikeCount() * 2 + post.getEmpathyCount() * 3 + post.getReplyCount() * 4 + 1;
    }

    private void refreshPostScore(TreeHolePost post) {
        double hours = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now()) + 2;
        double timeDecay = 1.0 / Math.pow(hours, 0.8);
        double engagement = post.getLikeCount() * 2 + post.getEmpathyCount() * 3 + post.getReplyCount() * 4 + post.getViewCount() * 0.5;
        post.setScore(engagement * timeDecay);
        postRepository.save(post);
    }

    private String normalizeEmotion(String emotion) {
        if (emotion == null || emotion.isBlank()) return "other";
        return emotion.toLowerCase().trim();
    }

    private boolean exceedsPostRateLimit(String anonymousHash) {
        return postRepository.countByAnonymousHashAndCreatedAtAfter(
                anonymousHash, LocalDateTime.now().minusHours(1)) >= POST_RATE_LIMIT_HOUR;
    }

    /**
     * IP 维度写限流（Redis 1 小时窗口）。
     * 身份级限流可被"轮换身份"绕过（旧格式 UUID 过渡期尤甚），IP 维度封住轮换收益。
     */
    private boolean exceedsIpWriteLimit(String clientIp, String action, int limitPerHour) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        try {
            String key = "treehole:ip:" + action + ":" + clientIp;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, java.time.Duration.ofHours(1));
            }
            return count != null && count > limitPerHour;
        } catch (Exception e) {
            // Redis 故障时降级为仅身份级限流
            return false;
        }
    }

    private boolean exceedsReplyRateLimit(String anonymousHash) {
        return replyRepository.countByAnonymousHashAndCreatedAtAfter(
                anonymousHash, LocalDateTime.now().minusHours(1)) >= REPLY_RATE_LIMIT_HOUR;
    }

    private boolean exceedsReactionRateLimit(String anonymousHash) {
        return reactionRepository.countByAnonymousHashAndCreatedAtAfter(
                anonymousHash, LocalDateTime.now().minusHours(1)) >= REACTION_RATE_LIMIT_HOUR;
    }

    private boolean exceedsReportRateLimit(String anonymousHash) {
        return reportRepository.countByAnonymousHashAndCreatedAtAfter(
                anonymousHash, LocalDateTime.now().minusDays(1)) >= REPORT_RATE_LIMIT_DAY;
    }

    public TreeHolePost getRawPost(Long id) {
        return postRepository.findById(id).orElse(null);
    }
}
