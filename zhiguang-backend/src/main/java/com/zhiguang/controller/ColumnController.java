package com.zhiguang.controller;

import com.zhiguang.dto.*;
import com.zhiguang.entity.User;
import com.zhiguang.service.ColumnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/columns")
@RequiredArgsConstructor
public class ColumnController {

    private final ColumnService columnService;

    /** 创建专栏 */
    @PostMapping
    public ResponseEntity<ApiResponse<ColumnResponseDto>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ColumnRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                columnService.createColumn(user.getId(), request, user)));
    }

    /** 编辑专栏 */
    @PutMapping("/{columnId}")
    public ResponseEntity<ApiResponse<ColumnResponseDto>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long columnId,
            @Valid @RequestBody ColumnRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                columnService.updateColumn(columnId, user.getId(), request)));
    }

    /** 公开：全部专栏 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ColumnResponseDto>>> getAllColumns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(columnService.getAllColumns(userId, page, size)));
    }

    /** 获取专栏详情 */
    @GetMapping("/{columnId}")
    public ResponseEntity<ApiResponse<ColumnResponseDto>> getById(
            @PathVariable Long columnId,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(columnService.getColumnById(columnId, userId)));
    }

    /** 获取用户的专栏列表 */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ColumnResponseDto>>> getUserColumns(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(
                columnService.getUserColumns(userId, currentUserId, page, size)));
    }

    /** 添加文章到专栏 */
    @PostMapping("/{columnId}/articles/{articleId}")
    public ResponseEntity<ApiResponse<Void>> addArticle(
            @AuthenticationPrincipal User user,
            @PathVariable Long columnId,
            @PathVariable Long articleId) {
        columnService.addArticleToColumn(columnId, articleId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("添加成功", null));
    }

    /** 从专栏移除文章 */
    @DeleteMapping("/{columnId}/articles/{articleId}")
    public ResponseEntity<ApiResponse<Void>> removeArticle(
            @AuthenticationPrincipal User user,
            @PathVariable Long columnId,
            @PathVariable Long articleId) {
        columnService.removeArticleFromColumn(columnId, articleId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("移除成功", null));
    }

    /** 获取专栏内的文章 */
    @GetMapping("/{columnId}/articles")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> getArticles(
            @PathVariable Long columnId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                columnService.getColumnArticles(columnId, page, size)));
    }

    /** 订阅专栏 */
    @PostMapping("/{columnId}/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @AuthenticationPrincipal User user,
            @PathVariable Long columnId) {
        columnService.subscribe(columnId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("订阅成功", null));
    }

    /** 取消订阅 */
    @DeleteMapping("/{columnId}/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @AuthenticationPrincipal User user,
            @PathVariable Long columnId) {
        columnService.unsubscribe(columnId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("取消订阅", null));
    }

    /** 删除专栏 */
    @DeleteMapping("/{columnId}")
    public ResponseEntity<ApiResponse<Void>> deleteColumn(
            @AuthenticationPrincipal User user,
            @PathVariable Long columnId) {
        columnService.deleteColumn(columnId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
