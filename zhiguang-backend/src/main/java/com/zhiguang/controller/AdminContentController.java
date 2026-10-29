package com.zhiguang.controller;

import com.zhiguang.dto.*;
import com.zhiguang.entity.SensitiveWord;
import com.zhiguang.entity.User;
import com.zhiguang.service.AdminContentService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService adminContentService;

    // ========== 文章审核 ==========

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<PageResponse<AdminArticleDto>>> getArticles(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<AdminArticleDto> response = adminContentService.getArticles(status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/articles/{articleId}/review")
    public ResponseEntity<ApiResponse<Void>> reviewArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody ArticleReviewDto dto,
            @AuthenticationPrincipal User admin) {
        adminContentService.reviewArticle(articleId, dto, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("审核完成", null));
    }

    // ========== 评论审核 ==========

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<PageResponse<AdminCommentDto>>> getComments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<AdminCommentDto> response = adminContentService.getComments(status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/comments/{commentId}/review")
    public ResponseEntity<ApiResponse<Void>> reviewComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReviewDto dto,
            @AuthenticationPrincipal User admin) {
        adminContentService.reviewComment(commentId, dto, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("审核完成", null));
    }

    // ========== 举报处理 ==========

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponseDto>>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<ReportResponseDto> response = adminContentService.getReports(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/reports/{reportId}/handle")
    public ResponseEntity<ApiResponse<Void>> handleReport(
            @PathVariable Long reportId,
            @Valid @RequestBody HandleReportDto dto,
            @AuthenticationPrincipal User admin) {
        adminContentService.handleReport(reportId, dto, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("举报已处理", null));
    }

    // ========== 敏感词管理 ==========

    @GetMapping("/sensitive-words")
    public ResponseEntity<ApiResponse<PageResponse<SensitiveWord>>> getSensitiveWords(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<SensitiveWord> response = adminContentService.getSensitiveWords(category, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/sensitive-words")
    public ResponseEntity<ApiResponse<SensitiveWord>> addSensitiveWord(
            @Valid @RequestBody SensitiveWordDto dto,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(ApiResponse.success("添加成功", adminContentService.addSensitiveWord(dto)));
    }

    @DeleteMapping("/sensitive-words/{wordId}")
    public ResponseEntity<ApiResponse<Void>> deleteSensitiveWord(
            @PathVariable Long wordId,
            @AuthenticationPrincipal User admin) {
        adminContentService.deleteSensitiveWord(wordId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ========== 敏感词检测（供前端发布时调用）==========

    @GetMapping("/check-sensitive")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSensitive(@RequestParam String text) {
        boolean contains = adminContentService.containsSensitiveWord(text);
        return ResponseEntity.ok(ApiResponse.success(Map.of("containsSensitiveWord", contains)));
    }
}
