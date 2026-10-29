package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.ArticleResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.service.ArticleService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FavoriteController {

    private final ArticleService articleService;

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponseDto>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        PageResponse<ArticleResponseDto> favorites = articleService.getUserFavorites(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }
}
