package com.zhiguang.service.agent.admin.tool;

import com.zhiguang.dto.ArticleReviewDto;
import com.zhiguang.dto.CommentReviewDto;
import com.zhiguang.dto.HandleReportDto;
import com.zhiguang.service.AdminContentService;
import com.zhiguang.service.agent.admin.AdminAgentTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 内容审核写操作（draft 型，管理员确认后执行）：文章审核 / 评论审核 / 举报处理。
 * 状态口径（知识库数据口径文档）：文章 APPROVE→2 / REJECT→4；评论 APPROVE→NORMAL / REJECT→DELETED；
 * 举报 HANDLED / DISMISSED。
 */
public class WriteReviewTools {

    private WriteReviewTools() {
    }

    private static long id(Map<String, Object> args) {
        Object v = args.get("id");
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    private static String str(Map<String, Object> args, String key) {
        return args.get(key) == null ? "" : String.valueOf(args.get(key));
    }

    @Component
    @RequiredArgsConstructor
    public static class ReviewArticleTool implements AdminAgentTool {
        private final AdminContentService adminContentService;

        @Override
        public String name() { return "review_article"; }

        @Override
        public String description() {
            return "审核文章（需管理员确认）。action=APPROVE 通过发布 / REJECT 拒绝下架。拒绝必须给 reason（依据审核政策）。审核前先用 query_content_detail 查看内容。";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "id", Map.of("type", "integer", "description", "文章 ID"),
                            "action", Map.of("type", "string", "enum", List.of("APPROVE", "REJECT")),
                            "reason", Map.of("type", "string", "description", "理由，REJECT 必填")),
                    "required", List.of("id", "action"));
        }

        @Override
        public boolean write() { return true; }

        @Override
        public String module() { return "CONTENT"; }

        @Override
        public String targetSummary(Map<String, Object> args) {
            return ("REJECT".equals(str(args, "action")) ? "拒绝（下架）" : "通过")
                    + "文章 #" + id(args) + (str(args, "reason").isEmpty() ? "" : "：" + str(args, "reason"));
        }

        @Override
        public String execute(Long adminId, Map<String, Object> args) throws Exception {
            // draft 参数校验：对象存在性（§13.3）
            return "{\"ok\":true,\"note\":\"已生成待确认操作\"}";
        }

        @Override
        public String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
            ArticleReviewDto dto = new ArticleReviewDto();
            dto.setStatus("REJECT".equals(str(args, "action")) ? 4 : 2);
            dto.setReason(str(args, "reason"));
            adminContentService.reviewArticle(id(args), dto, adminId);
            return "文章 #" + id(args) + " 已" + (dto.getStatus() == 2 ? "通过" : "下架");
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class ReviewCommentTool implements AdminAgentTool {
        private final AdminContentService adminContentService;

        @Override
        public String name() { return "review_comment"; }

        @Override
        public String description() {
            return "审核评论（需管理员确认）。action=APPROVE 恢复 NORMAL / REJECT 删除 DELETED。";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "id", Map.of("type", "integer", "description", "评论 ID"),
                            "action", Map.of("type", "string", "enum", List.of("APPROVE", "REJECT")),
                            "reason", Map.of("type", "string")),
                    "required", List.of("id", "action"));
        }

        @Override
        public boolean write() { return true; }

        @Override
        public String module() { return "CONTENT"; }

        @Override
        public String targetSummary(Map<String, Object> args) {
            return ("REJECT".equals(str(args, "action")) ? "删除" : "恢复") + "评论 #" + id(args);
        }

        @Override
        public String execute(Long adminId, Map<String, Object> args) throws Exception {
            return "{\"ok\":true,\"note\":\"已生成待确认操作\"}";
        }

        @Override
        public String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
            CommentReviewDto dto = new CommentReviewDto();
            dto.setStatus("REJECT".equals(str(args, "action")) ? "DELETED" : "NORMAL");
            adminContentService.reviewComment(id(args), dto, adminId);
            return "评论 #" + id(args) + " 已" + ("DELETED".equals(dto.getStatus()) ? "删除" : "恢复");
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class HandleReportTool implements AdminAgentTool {
        private final AdminContentService adminContentService;

        @Override
        public String name() { return "handle_report"; }

        @Override
        public String description() {
            return "处理内容举报（需管理员确认）。action=HANDLED 处置有效 / DISMISSED 驳回举报，note 必填处理说明。";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "id", Map.of("type", "integer", "description", "举报 ID"),
                            "action", Map.of("type", "string", "enum", List.of("HANDLED", "DISMISSED")),
                            "note", Map.of("type", "string", "description", "处理说明，必填")),
                    "required", List.of("id", "action", "note"));
        }

        @Override
        public boolean write() { return true; }

        @Override
        public String module() { return "CONTENT"; }

        @Override
        public String targetSummary(Map<String, Object> args) {
            return ("DISMISSED".equals(str(args, "action")) ? "驳回" : "处置") + "举报 #" + id(args)
                    + "：" + str(args, "note");
        }

        @Override
        public String execute(Long adminId, Map<String, Object> args) throws Exception {
            return "{\"ok\":true,\"note\":\"已生成待确认操作\"}";
        }

        @Override
        public String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
            HandleReportDto dto = new HandleReportDto();
            dto.setStatus(str(args, "action"));
            dto.setHandleResult(str(args, "note"));
            adminContentService.handleReport(id(args), dto, adminId);
            return "举报 #" + id(args) + " 已" + str(args, "action");
        }
    }
}
