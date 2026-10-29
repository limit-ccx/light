package com.zhiguang.controller;

import com.zhiguang.dto.*;
import com.zhiguang.entity.User;
import com.zhiguang.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionResponseDto>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CollectionRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.create(user.getId(), request, user)));
    }

    @PutMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponseDto>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long collectionId,
            @Valid @RequestBody CollectionRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.update(collectionId, user.getId(), request)));
    }

    /** 公开：全部合集 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CollectionResponseDto>>> getAllCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getAllCollections(page, size)));
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponseDto>> getById(@PathVariable Long collectionId) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getById(collectionId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<CollectionResponseDto>>> getUserCollections(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getUserCollections(userId, page, size)));
    }

    @PostMapping("/{collectionId}/articles/{articleId}")
    public ResponseEntity<ApiResponse<Void>> addArticle(
            @AuthenticationPrincipal User user,
            @PathVariable Long collectionId,
            @PathVariable Long articleId) {
        collectionService.addArticle(collectionId, articleId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("添加成功", null));
    }

    @DeleteMapping("/{collectionId}/articles/{articleId}")
    public ResponseEntity<ApiResponse<Void>> removeArticle(
            @AuthenticationPrincipal User user,
            @PathVariable Long collectionId,
            @PathVariable Long articleId) {
        collectionService.removeArticle(collectionId, articleId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("移除成功", null));
    }

    @GetMapping("/{collectionId}/articles")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> getArticles(
            @PathVariable Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(collectionService.getArticles(collectionId, page, size)));
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long collectionId) {
        collectionService.delete(collectionId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
