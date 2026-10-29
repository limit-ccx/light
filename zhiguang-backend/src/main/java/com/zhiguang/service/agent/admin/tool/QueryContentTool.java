package com.zhiguang.service.agent.admin.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.entity.Article;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.TreeHolePostRepository;
import com.zhiguang.service.agent.admin.AdminAgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 内容详情查询：文章（审核辅助）/ 树洞帖。返回摘要而非全文（§12.3 观察瘦身）。 */
@Component
@RequiredArgsConstructor
public class QueryContentTool implements AdminAgentTool {

    private final ArticleRepository articleRepository;
    private final TreeHolePostRepository treeHolePostRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "query_content_detail";
    }

    @Override
    public String description() {
        return "查询内容详情用于审核判断：type=article&id=文章ID 返回标题/作者/状态/正文摘要（前500字）/互动数；type=tree_hole&id=帖子ID 返回树洞帖内容与状态。审核前必须先查询内容。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "contentType", Map.of("type", "string", "enum", List.of("article", "tree_hole")),
                        "id", Map.of("type", "integer", "description", "内容 ID")),
                "required", List.of("contentType", "id"));
    }

    @Override
    public String execute(Long adminId, Map<String, Object> args) throws Exception {
        String type = String.valueOf(args.getOrDefault("contentType", ""));
        long id = parseLong(args.get("id"), -1);
        if (id <= 0) {
            return "{\"error\":\"invalid id\"}";
        }
        if ("tree_hole".equals(type)) {
            return treeHolePostRepository.findById(id)
                    .map(post -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", post.getId());
                        m.put("status", post.getStatus());
                        m.put("emotionTag", post.getEmotionTag());
                        String content = post.getContent() == null ? "" : post.getContent();
                        m.put("content", content.length() > 500 ? content.substring(0, 500) + "…" : content);
                        m.put("createdAt", String.valueOf(post.getCreatedAt()));
                        return toJson(m);
                    })
                    .orElse("{\"error\":\"not found\",\"id\":" + id + "}");
        }
        // article
        return articleRepository.findById(id)
                .map(article -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", article.getId());
                    m.put("title", article.getTitle());
                    m.put("status", article.getStatus());
                    m.put("author", article.getAuthor() == null ? null : article.getAuthor().getUsername());
                    String content = article.getContent() == null ? "" : article.getContent().replaceAll("<[^>]*>", "");
                    m.put("contentPreview", content.length() > 500 ? content.substring(0, 500) + "…" : content);
                    m.put("viewCount", article.getViewCount());
                    m.put("likeCount", article.getLikeCount());
                    m.put("createdAt", String.valueOf(article.getCreatedAt()));
                    return toJson(m);
                })
                .orElse("{\"error\":\"not found\",\"id\":" + id + "}");
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
