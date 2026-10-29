package com.zhiguang.controller;

import com.zhiguang.dto.*;
import com.zhiguang.entity.User;
import com.zhiguang.service.DocumentParserService;
import com.zhiguang.service.KnowledgeService;
import com.zhiguang.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final RagService ragService;
    private final DocumentParserService documentParserService;

    private User getCurrentUser(User user) {
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }

    // ==================== 知识库条目管理 ====================

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<KnowledgeItemDto>> addToKnowledge(
            @AuthenticationPrincipal User user,
            @RequestBody AddToKnowledgeRequest request) {
        User currentUser = getCurrentUser(user);
        KnowledgeItemDto item = knowledgeService.addArticleToKnowledge(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("已添加到知识库", item));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeItemDto>>> getKnowledgeItems(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId) {
        User currentUser = getCurrentUser(user);
        PageResponse<KnowledgeItemDto> response = knowledgeService.getKnowledgeItems(currentUser.getId(), categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/items/search")
    public ResponseEntity<ApiResponse<List<KnowledgeItemDto>>> searchKnowledgeItems(
            @AuthenticationPrincipal User user,
            @RequestParam String keyword) {
        User currentUser = getCurrentUser(user);
        List<KnowledgeItemDto> items = knowledgeService.searchKnowledgeItems(currentUser.getId(), keyword);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<KnowledgeItemDto>> getKnowledgeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        User currentUser = getCurrentUser(user);
        KnowledgeItemDto item = knowledgeService.getKnowledgeItem(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<KnowledgeItemDto>> updateKnowledgeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody AddToKnowledgeRequest request) {
        User currentUser = getCurrentUser(user);
        KnowledgeItemDto item = knowledgeService.updateKnowledgeItem(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", item));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledgeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        User currentUser = getCurrentUser(user);
        knowledgeService.deleteKnowledgeItem(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("已从知识库移除", null));
    }

    @PutMapping("/items/{id}/read-status")
    public ResponseEntity<ApiResponse<KnowledgeItemDto>> updateReadStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        User currentUser = getCurrentUser(user);
        String status = request.get("status");
        KnowledgeItemDto item = knowledgeService.updateReadStatus(currentUser.getId(), id, status);
        return ResponseEntity.ok(ApiResponse.success("阅读状态已更新", item));
    }

    // ==================== 分类管理 ====================

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<KnowledgeCategoryDto>> createCategory(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {
        User currentUser = getCurrentUser(user);
        String name = (String) request.get("name");
        Long parentId = request.get("parentId") != null ? Long.valueOf(request.get("parentId").toString()) : null;
        String icon = (String) request.get("icon");
        String color = (String) request.get("color");

        KnowledgeCategoryDto category = knowledgeService.createCategory(currentUser.getId(), name, parentId, icon, color);
        return ResponseEntity.ok(ApiResponse.success("分类创建成功", category));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<KnowledgeCategoryDto>>> getCategories(
            @AuthenticationPrincipal User user) {
        User currentUser = getCurrentUser(user);
        List<KnowledgeCategoryDto> categories = knowledgeService.getCategories(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        User currentUser = getCurrentUser(user);
        knowledgeService.deleteCategory(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("分类已删除", null));
    }

    // ==================== 标签管理 ====================

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<String>>> getAllTags(
            @AuthenticationPrincipal User user) {
        User currentUser = getCurrentUser(user);
        List<String> tags = knowledgeService.getAllTags(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    // ==================== 统计 ====================

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getKnowledgeCount(
            @AuthenticationPrincipal User user) {
        User currentUser = getCurrentUser(user);
        long count = knowledgeService.getKnowledgeItemCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    // ==================== 导入本地文档 ====================

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<KnowledgeItemDto>> importDocument(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) List<String> customTags) {
        User currentUser = getCurrentUser(user);

        // 检查文件类型
        if (!documentParserService.isSupported(file.getContentType(), file.getOriginalFilename())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "不支持的文件类型，请上传 txt, md, pdf, doc, docx, xls, xlsx, ppt, pptx, html 格式的文件"));
        }

        // 解析文档
        DocumentParserService.ParsedDocument parsed = documentParserService.parse(file);

        // 导入到知识库
        ImportDocumentRequest request = new ImportDocumentRequest();
        request.setTitle(parsed.title());
        request.setContent(parsed.content());
        request.setSummary(parsed.summary());
        request.setCategoryId(categoryId);
        request.setNotes(notes);
        request.setCustomTags(customTags);

        KnowledgeItemDto item = knowledgeService.importDocument(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("文档导入成功", item));
    }

    // ==================== RAG问答 ====================

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<RagAnswerDto>> askKnowledge(
            @AuthenticationPrincipal User user,
            @RequestBody RagAskRequest request) {
        User currentUser = getCurrentUser(user);
        RagAnswerDto answer = ragService.askKnowledge(currentUser.getId(), request.getQuestion(), request.getItemIds());
        return ResponseEntity.ok(ApiResponse.success(answer));
    }
}
