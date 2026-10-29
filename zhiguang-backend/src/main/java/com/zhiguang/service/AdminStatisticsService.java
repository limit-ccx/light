package com.zhiguang.service;

import com.zhiguang.dto.AdminDashboardDto;
import com.zhiguang.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;

    /**
     * 获取仪表盘概览数据（扁平结构，直接包含趋势百分比）
     */
    public AdminDashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        // 总数
        long totalUsers = userRepository.count();
        long articleCount = articleRepository.countByStatus(2); // 已发布
        long commentCount = commentRepository.count();
        long likeCount = articleLikeRepository.count();
        long favoriteCount = articleFavoriteRepository.count();
        long bannedUsers = userRepository.countByStatus("BANNED");
        long pendingReviewArticles = articleRepository.countByStatus(3);
        long pendingReviewComments = commentRepository.countByStatus("PENDING");

        // 今日新增
        long todayNewUsers = userRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long todayNewArticles = articleRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long todayNewComments = commentRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long todayNewLikes = articleLikeRepository.countByCreatedAtBetween(todayStart, tomorrowStart);

        // 昨日新增（用于计算趋势）
        long yesterdayNewUsers = userRepository.countByCreatedAtBetween(yesterdayStart, todayStart);
        long yesterdayNewArticles = articleRepository.countByCreatedAtBetween(yesterdayStart, todayStart);
        long yesterdayNewComments = commentRepository.countByCreatedAtBetween(yesterdayStart, todayStart);
        long yesterdayNewLikes = articleLikeRepository.countByCreatedAtBetween(yesterdayStart, todayStart);

        return AdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .todayNewUsers(todayNewUsers)
                .articleCount(articleCount)
                .commentCount(commentCount)
                .likeCount(likeCount)
                .favoriteCount(favoriteCount)
                .aiCallCount(0) // 暂无 AI 调用统计，可后续扩展
                .pendingReviewArticles(pendingReviewArticles)
                .pendingReviewComments(pendingReviewComments)
                .bannedUsers(bannedUsers)
                .todayNewArticles(todayNewArticles)
                .todayNewComments(todayNewComments)
                .userTrend(calcTrend(todayNewUsers, yesterdayNewUsers))
                .newUserTrend(calcTrend(todayNewUsers, yesterdayNewUsers))
                .articleTrend(calcTrend(todayNewArticles, yesterdayNewArticles))
                .commentTrend(calcTrend(todayNewComments, yesterdayNewComments))
                .likeTrend(calcTrend(todayNewLikes, yesterdayNewLikes))
                .aiTrend(0)
                .build();
    }

    /**
     * 计算趋势百分比：(today - yesterday) / yesterday * 100
     */
    private double calcTrend(long today, long yesterday) {
        if (yesterday == 0) return today > 0 ? 100.0 : 0.0;
        return Math.round((today - yesterday) * 1000.0 / yesterday) / 10.0;
    }

    /**
     * 获取用户增长统计（最近 N 天）
     */
    public Map<String, Object> getUserGrowth(int days) {
        LocalDate now = LocalDate.now();
        LocalDate since = now.minusDays(days);

        long totalUsers = userRepository.count();
        long newUsersInPeriod = userRepository.countByCreatedAtAfter(since.atStartOfDay());

        // 按日统计新增用户数（单条 GROUP BY，替代此前"每天一次 COUNT"的 N 次查询）
        java.util.Map<String, Long> countByDate = new java.util.HashMap<>();
        for (Object[] row : userRepository.countDailyNewUsers(since.atStartOfDay())) {
            java.sql.Date day = (java.sql.Date) row[0];
            Number cnt = (Number) row[1];
            if (day != null && cnt != null) {
                countByDate.put(day.toLocalDate().toString(), cnt.longValue());
            }
        }
        List<String> dates = new java.util.ArrayList<>();
        List<Long> dailyNewUsers = new java.util.ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            dates.add(date.toString());
            dailyNewUsers.add(countByDate.getOrDefault(date.toString(), 0L));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("newUsersInPeriod", newUsersInPeriod);
        result.put("days", days);
        result.put("dates", dates);
        result.put("dailyNewUsers", dailyNewUsers);
        return result;
    }

    /**
     * 获取内容统计
     */
    public Map<String, Object> getContentStats() {
        long publishedArticles = articleRepository.countByStatus(2);
        long draftArticles = articleRepository.countByStatus(1);
        long pendingArticles = articleRepository.countByStatus(3);
        long takedownArticles = articleRepository.countByStatus(4);
        long totalComments = commentRepository.count();
        long normalComments = commentRepository.countByStatus("NORMAL");
        long pendingComments = commentRepository.countByStatus("PENDING");
        long deletedComments = commentRepository.countByStatus("DELETED");

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("已发布文章", publishedArticles);
        result.put("草稿", draftArticles);
        result.put("待审核文章", pendingArticles);
        result.put("已下架文章", takedownArticles);
        result.put("总评论数", totalComments);
        result.put("正常评论", normalComments);
        result.put("待审核评论", pendingComments);
        result.put("已删除评论", deletedComments);
        return result;
    }
}
