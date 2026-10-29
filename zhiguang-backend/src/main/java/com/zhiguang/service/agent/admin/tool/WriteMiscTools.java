package com.zhiguang.service.agent.admin.tool;

import com.zhiguang.dto.FeaturedContentDto;
import com.zhiguang.entity.TreeHolePost;
import com.zhiguang.repository.TreeHolePostRepository;
import com.zhiguang.service.AdminLogService;
import com.zhiguang.service.AdminRecommendService;
import com.zhiguang.service.FeedbackService;
import com.zhiguang.service.agent.admin.AdminAgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 其余写操作（draft 型）：树洞管理 / 内容推荐 / 建议反馈处理。
 */
public class WriteMiscTools {

    private WriteMiscTools() {
    }

    private static long id(Map<String, Object> args) {
        Object v = args.get("id");
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    private static String str(Map<String, Object> args, String key) {
        return args.get(key) == null ? "" : String.valueOf(args.get(key));
    }

    /** 树洞管理：通过/驳回/删除（复刻 TreeHoleAdminController 的直改逻辑 + 审计） */
    @Component
    @RequiredArgsConstructor
    public static class ModerateTreeHoleTool implements AdminAgentTool {
        private final TreeHolePostRepository postRepository;
        private final AdminLogService adminLogService;

        @Override
        public String name() { return "moderate_tree_hole"; }

        @Override
        public String description() {
            return "树洞帖管理（需管理员确认）。action=APPROVE 审核通过 / REJECT 驳回 / DELETE 删除（线上内容紧急处置）。REJECT/DELETE 必填 reason。处置前先 query_content_detail 查看内容。";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "id", Map.of("type", "integer", "description", "树洞帖 ID"),
                            "action", Map.of("type", "string", "enum", List.of("APPROVE", "REJECT", "DELETE")),
                            "reason", Map.of("type", "string", "description", "理由，REJECT/DELETE 必填")),
                    "required", List.of("id", "action"));
        }

        @Override
        public boolean write() { return true; }

        @Override
        public String module() { return "COMMUNITY"; }

        @Override
        public String targetSummary(Map<String, Object> args) {
            String label = switch (str(args, "action")) {
                case "APPROVE" -> "通过";
                case "DELETE" -> "删除";
                default -> "驳回";
            };
            return label + "树洞帖 #" + id(args) + (str(args, "reason").isEmpty() ? "" : "：" + str(args, "reason"));
        }

        @Override
        public String execute(Long adminId, Map<String, Object> args) throws Exception {
            return "{\"ok\":true,\"note\":\"已生成待确认操作\"}";
        }

        @Override
        public String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
            long postId = id(args);
            TreeHolePost post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("树洞帖不存在: " + postId));
            String action = str(args, "action");
            switch (action) {
                case "APPROVE" -> {
                    post.setStatus(1);
                    postRepository.save(post);
                    adminLogService.log(adminId, "TREEHOLE_APPROVE_POST", "TREEHOLE_POST", postId, "Agent: 审核通过");
                }
                case "REJECT" -> {
                    post.setStatus(2);
                    postRepository.save(post);
                    adminLogService.log(adminId, "TREEHOLE_REJECT_POST", "TREEHOLE_POST", postId,
                            "Agent: 驳回, 原因: " + str(args, "reason"));
                }
                case "DELETE" -> {
                    postRepository.deleteById(postId);
                    adminLogService.log(adminId, "TREEHOLE_DELETE_POST", "TREEHOLE_POST", postId,
                            "Agent: 删除, 原因: " + str(args, "reason"));
                }
                default -> throw new IllegalArgumentException("未知动作: " + action);
            }
            return "树洞帖 #" + postId + " 已" + targetSummary(args).replaceAll("树洞帖.*", "");
        }
    }

    /** 推荐位设置/取消（复用 AdminRecommendService） */
    @Component
    @RequiredArgsConstructor
    public static class SetRecommendationTool implements AdminAgentTool {
        private final AdminRecommendService recommendService;

        @Override
        public String name() { return "set_recommendation"; }

        @Override
        public String description() {
            return "设置或取消首页推荐位（需管理员确认）。on=true 时将文章加入推荐（position 默认 HOME）；on=false 时移除该文章的推荐。";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "articleId", Map.of("type", "integer", "description", "文章 ID"),
                            "on", Map.of("type", "boolean", "description", "true 加入推荐，false 取消推荐"),
                            "position", Map.of("type", "string", "description", "推荐位，默认 HOME")),
                    "required", List.of("articleId", "on"));
        }

        @Override
        public boolean write() { return true; }

        @Override
        public String module() { return "CONTENT"; }

        @Override
        public String targetSummary(Map<String, Object> args) {
            boolean on = Boolean.parseBoolean(String.valueOf(args.get("on")));
            return (on ? "加入" : "取消") + "文章 #" + id(Map.of("id", args.get("articleId"))) + " 的首页推荐";
        }

        @Override
        public String execute(Long adminId, Map<String, Object> args) throws Exception {
            return "{\"ok\":true,\"note\":\"已生成待确认操作\"}";
        }

        @Override
        public String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
            long articleId = id(Map.of("id", args.get("articleId")));
            boolean on = Boolean.parseBoolean(String.valueOf(args.get("on")));
            String position = args.get("position") == null ? "HOME" : String.valueOf(args.get("position"));
            if (on) {
                FeaturedContentDto dto = new FeaturedContentDto();
                dto.setTargetType("ARTICLE");
                dto.setTargetId(articleId);
                dto.setPosition(position);
                recommendService.addFeaturedContent(dto, adminId);
                return "文章 #" + articleId + " 已加入 " + position + " 推荐位";
            }
            // off：在列表中找到该文章的推荐条目并移除
            var page = recommendService.getFeaturedContent(position, 0, 200);
            boolean removed = false;
            for (Object item : page.getList()) {
                try {
                    Long itemId = (Long) item.getClass().getMethod("getId").invoke(item);
                    Long targetId = (Long) item.getClass().getMethod("getTargetId").invoke(item);
                    String type = (String) item.getClass().getMethod("getTargetType").invoke(item);
                    if ("ARTICLE".equals(type) && targetId != null && targetId == articleId) {
                        recommendService.removeFeaturedContent(itemId, adminId);
                        removed = true;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            if (!removed) {
                return "文章 #" + articleId + " 不在 " + position + " 推荐位（可能已移除）";
            }
            return "文章 #" + articleId + " 已从推荐位移除";
        }
    }

    /** 建议反馈处理（复用 FeedbackService.handle） */
    @Component
    @RequiredArgsConstructor
    public static class ResolveFeedbackTool implements AdminAgentTool {
        private final FeedbackService feedbackService;

        @Override
        public String name() { return "resolve_feedback"; }

        @Override
        public String description() {
            return "处理建议反馈（需管理员确认）：标记为已处理并附回复（reply 会展示给提交者）。";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "id", Map.of("type", "integer", "description", "反馈 ID"),
                            "reply", Map.of("type", "string", "description", "给提交者的回复，建议填写")),
                    "required", List.of("id"));
        }

        @Override
        public boolean write() { return true; }

        @Override
        public String targetSummary(Map<String, Object> args) {
            return "处理反馈 #" + id(args) + (str(args, "reply").isEmpty() ? "" : "：" + str(args, "reply"));
        }

        @Override
        public String execute(Long adminId, Map<String, Object> args) throws Exception {
            return "{\"ok\":true,\"note\":\"已生成待确认操作\"}";
        }

        @Override
        public String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
            feedbackService.handle(id(args), "HANDLED", str(args, "reply"));
            return "反馈 #" + id(args) + " 已标记处理";
        }
    }
}
