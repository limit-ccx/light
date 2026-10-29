package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.CommentRequestDto;
import com.zhiguang.dto.CommentResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.service.CommentService;
import com.zhiguang.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId}")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponseDto>>> getCommentsByArticle(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<CommentResponseDto> comments = commentService.getCommentsByArticleId(articleId, user, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CommentRequestDto request) {
        if (request.getArticleId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文章ID不能为空"));
        }
        CommentResponseDto comment = commentService.createComment(request.getArticleId(), user, request);
        return ResponseEntity.ok(ApiResponse.success("评论发布成功", comment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        commentService.deleteComment(id, user);
        return ResponseEntity.ok(ApiResponse.success("评论删除成功", null));
    }
}
