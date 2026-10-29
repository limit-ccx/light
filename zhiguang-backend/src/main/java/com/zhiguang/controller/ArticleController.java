package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.ArticleRequestDto;
import com.zhiguang.dto.ArticleResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.service.ArticleService;
import com.zhiguang.service.LikeService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final LikeService likeService;

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTagCloud() {
        List<Map<String, Object>> tags = articleService.getTagCloud();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> getFollowingFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BadRequestException("请先登录");
        }
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<ArticleResponseDto> response = articleService.getFollowingFeed(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search) {

        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);

        PageResponse<ArticleResponseDto> response;

        if (search != null && !search.isBlank()) {
            response = articleService.searchArticles(search, page, size, sortBy);
        } else if (tag != null && !tag.isBlank()) {
            response = articleService.getArticlesByTags(Arrays.asList(tag), page, size, sortBy);
        } else {
            response = articleService.getArticles(page, size, sortBy);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponseDto>> getArticleById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        ArticleResponseDto article = articleService.getArticleById(id, user);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponseDto>> createArticle(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ArticleRequestDto request) {
        ArticleResponseDto article = articleService.createArticle(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("文章创建成功", article));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponseDto>> updateArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ArticleRequestDto request) {
        ArticleResponseDto article = articleService.updateArticle(id, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("文章更新成功", article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        articleService.deleteArticle(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("文章删除成功", null));
    }

    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<ArticleResponseDto>>> getHotArticles(
            @RequestParam(defaultValue = "5") int limit) {
        List<ArticleResponseDto> articles = articleService.getHotArticles(limit);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<ArticleResponseDto>>> getPopularArticles(
            @RequestParam(defaultValue = "5") int limit) {
        List<ArticleResponseDto> articles = articleService.getPopularArticles(limit);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> getArticlesByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<ArticleResponseDto> response = articleService.getArticlesByAuthor(userId, user, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Map<String, Object> result = likeService.toggleLike(id, user);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Map<String, Object> result = likeService.toggleFavorite(id, user);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/like/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLikeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Long userId = user == null ? null : user.getId();
        Map<String, Object> result = likeService.getLikeStatus(id, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
