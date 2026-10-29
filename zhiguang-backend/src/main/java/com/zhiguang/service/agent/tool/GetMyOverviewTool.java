package com.zhiguang.service.agent.tool;

import com.zhiguang.entity.User;
import com.zhiguang.repository.ArticleFavoriteRepository;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.UserLevelRepository;
import com.zhiguang.service.SignInService;
import com.zhiguang.service.UserService;
import com.zhiguang.service.agent.AgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前用户概览：昵称、等级、发文数、收藏数、连续签到天数。
 * 用于个性化引导（"你还没收藏过文章，试试…"）。数据一律以服务端 userId 取得。
 */
@Component
@RequiredArgsConstructor
public class GetMyOverviewTool implements AgentTool {

    private final UserService userService;
    private final ArticleRepository articleRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final UserLevelRepository userLevelRepository;
    private final SignInService signInService;

    @Override
    public String name() {
        return "get_my_overview";
    }

    @Override
    public String description() {
        return "获取当前用户自己的概览信息（昵称、等级、发文数、收藏数、连续签到天数）。当用户问'我的''我发了几篇''我的等级'等关于自己的问题时调用。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public String execute(Long userId, Map<String, Object> args) throws Exception {
        User user = userService.getUserById(userId);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("username", user.getUsername());
        overview.put("joinDays", user.getCreatedAt() == null
                ? null : java.time.temporal.ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now()));

        try {
            overview.put("publishedArticles", articleRepository.countByAuthorIdAndStatus(userId, 2));
        } catch (Exception e) {
            overview.put("publishedArticles", null);
        }
        try {
            overview.put("favorites", articleFavoriteRepository.countByUser_Id(userId));
        } catch (Exception e) {
            overview.put("favorites", null);
        }
        try {
            userLevelRepository.findByUserId(userId).ifPresent(level -> {
                overview.put("level", level.getLevel());
                overview.put("experience", level.getExperience());
            });
        } catch (Exception ignored) {
        }
        try {
            LocalDate today = LocalDate.now();
            overview.put("continuousSignInDays", signInService
                    .getCalendar(userId, today.getYear(), today.getMonthValue()).getContinuousDays());
        } catch (Exception ignored) {
        }

        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(overview);
    }
}
