package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private long totalUsers;
    private long todayNewUsers;
    private long articleCount;
    private long commentCount;
    private long likeCount;
    private long favoriteCount;
    private long aiCallCount;

    private long pendingReviewArticles;
    private long pendingReviewComments;
    private long bannedUsers;

    // 今日新增
    private long todayNewArticles;
    private long todayNewComments;

    // 趋势（百分比，较昨日）
    private double userTrend;
    private double newUserTrend;
    private double articleTrend;
    private double commentTrend;
    private double likeTrend;
    private double aiTrend;
}
