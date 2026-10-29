package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.ArticleResponseDto;
import com.zhiguang.dto.ArticleVersionDto;
import com.zhiguang.entity.Article;
import com.zhiguang.entity.User;
import com.zhiguang.service.ArticleService;
import com.zhiguang.service.ArticleVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleVersionController {

    private final ArticleVersionService versionService;
    private final ArticleService articleService;

    /** 获取文章历史版本列表 */
    @GetMapping("/{articleId}/versions")
    public ResponseEntity<ApiResponse<List<ArticleVersionDto>>> getVersions(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId) {
        return ResponseEntity.ok(ApiResponse.success(
                versionService.getVersions(articleId, user.getId())));
    }

    /** 获取单个版本详情 */
    @GetMapping("/{articleId}/versions/{versionId}")
    public ResponseEntity<ApiResponse<ArticleVersionDto>> getVersion(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(
                versionService.getVersion(versionId, user.getId())));
    }

    /** 恢复到指定版本 */
    @PostMapping("/{articleId}/versions/{versionId}/restore")
    public ResponseEntity<ApiResponse<ArticleResponseDto>> restoreVersion(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId,
            @PathVariable Long versionId) {
        Article article = versionService.restoreVersion(articleId, versionId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticleById(articleId, user)));
    }
}
