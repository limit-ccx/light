package com.zhiguang.service.agent.admin.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.CommentRepository;
import com.zhiguang.repository.ReportRepository;
import com.zhiguang.repository.TreeHolePostRepository;
import com.zhiguang.service.FeedbackService;
import com.zhiguang.service.agent.admin.AdminAgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据查询工具：平台统计指标 + 待审核队列计数（复用 AdminStatisticsService 与各 repository count）。
 */
@Component
@RequiredArgsConstructor
public class QueryStatsTool implements AdminAgentTool {

    private final com.zhiguang.service.AdminStatisticsService statisticsService;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final TreeHolePostRepository treeHolePostRepository;
    private final ReportRepository reportRepository;
    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "query_statistics";
    }

    @Override
    public String description() {
        return "查询平台统计数据。metric=dashboard 返回核心指标总览；metric=growth&days=N 返回近 N 天用户增长（默认7）；metric=content 返回内容统计；metric=queue 返回各待审核队列计数。数字必须原样使用，禁止估算。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "metric", Map.of("type", "string", "enum", List.of("dashboard", "growth", "content", "queue"),
                                "description", "查询哪类指标"),
                        "days", Map.of("type", "integer", "description", "metric=growth 时的天数，默认 7，最大 30")),
                "required", List.of("metric"));
    }

    @Override
    public String execute(Long adminId, Map<String, Object> args) throws Exception {
        String metric = String.valueOf(args.getOrDefault("metric", "dashboard"));
        Object data = switch (metric) {
            case "growth" -> {
                int days = parseInt(args.get("days"), 7);
                yield statisticsService.getUserGrowth(Math.min(Math.max(days, 1), 30));
            }
            case "content" -> statisticsService.getContentStats();
            case "queue" -> reviewQueue();
            default -> statisticsService.getDashboard();
        };
        return objectMapper.writeValueAsString(data);
    }

    private Map<String, Object> reviewQueue() {
        Map<String, Object> queue = new LinkedHashMap<>();
        queue.put("pendingArticles", articleRepository.countByStatus(3));
        queue.put("pendingComments", commentRepository.countByStatus("PENDING"));
        queue.put("pendingTreeHolePosts", treeHolePostRepository.countByStatus(0));
        queue.put("pendingReports", reportRepository.countByStatus("PENDING"));
        try {
            queue.putAll(feedbackService.stats());
        } catch (Exception ignored) {
        }
        return queue;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
