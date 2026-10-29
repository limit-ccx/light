package com.zhiguang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.dto.*;
import com.zhiguang.entity.*;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final KnowledgeCategoryRepository knowledgeCategoryRepository;
    private final ArticleRepository articleRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // ==================== 知识库条目管理 ====================

    @Transactional
    public KnowledgeItemDto addArticleToKnowledge(Long userId, AddToKnowledgeRequest request) {
        User user = userService.getUserById(userId);
        Article article = articleRepository.findById(request.getArticleId())
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", request.getArticleId()));

        // 检查是否已添加
        if (knowledgeItemRepository.existsByUserIdAndOriginalArticleId(userId, request.getArticleId())) {
            throw new BadRequestException("该文章已在知识库中");
        }

        KnowledgeItem item = new KnowledgeItem();
        item.setUser(user);
        item.setOriginalArticleId(article.getId());

        // 完整缓存文章内容
        item.setCachedTitle(article.getTitle());
        item.setCachedContent(article.getContent());
        item.setCachedSummary(article.getSummary());
        item.setCachedCover(article.getCover());
        item.setCachedTags(convertTagsToJson(article.getTags()));
        item.setCachedAuthorName(article.getAuthor().getUsername());
        item.setCachedAuthorAvatar(article.getAuthor().getAvatar());

        // 用户自定义设置
        if (request.getCategoryId() != null) {
            KnowledgeCategory category = knowledgeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", "id", request.getCategoryId()));
            item.setCategory(category);
        }
        item.setNotes(request.getNotes());
        if (request.getCustomTags() != null) {
            item.setCustomTags(request.getCustomTags());
        }

        KnowledgeItem saved = knowledgeItemRepository.save(item);
        return toItemDto(saved);
    }

    @Transactional
    public KnowledgeItemDto importDocument(Long userId, ImportDocumentRequest request) {
        User user = userService.getUserById(userId);

        KnowledgeItem item = new KnowledgeItem();
        item.setUser(user);
        item.setOriginalArticleId(null); // 本地文档没有原文章ID

        // 设置文档内容
        item.setCachedTitle(request.getTitle());
        item.setCachedContent(request.getContent());
        item.setCachedSummary(request.getSummary());
        item.setCachedTags("[]");
        item.setCachedAuthorName(user.getUsername());
        item.setCachedAuthorAvatar(user.getAvatar());

        // 用户自定义设置
        if (request.getCategoryId() != null) {
            KnowledgeCategory category = knowledgeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", "id", request.getCategoryId()));
            item.setCategory(category);
        }
        item.setNotes(request.getNotes());
        if (request.getCustomTags() != null) {
            item.setCustomTags(request.getCustomTags());
        }

        KnowledgeItem saved = knowledgeItemRepository.save(item);
        return toItemDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeItemDto> getKnowledgeItems(Long userId, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<KnowledgeItem> itemPage;

        if (categoryId != null) {
            itemPage = knowledgeItemRepository.findByUserIdAndCategoryIdOrderByCreatedAtDesc(userId, categoryId, pageable);
        } else {
            itemPage = knowledgeItemRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<KnowledgeItemDto> list = itemPage.getContent().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                list,
                itemPage.getTotalElements(),
                itemPage.getNumber(),
                itemPage.getSize(),
                itemPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<KnowledgeItemDto> searchKnowledgeItems(Long userId, String keyword) {
        List<KnowledgeItem> items = knowledgeItemRepository.searchByKeyword(userId, keyword);
        return items.stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KnowledgeItemDto getKnowledgeItem(Long userId, Long itemId) {
        KnowledgeItem item = knowledgeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库条目", "id", itemId));
        return toItemDto(item);
    }

    @Transactional
    public KnowledgeItemDto updateKnowledgeItem(Long userId, Long itemId, AddToKnowledgeRequest request) {
        KnowledgeItem item = knowledgeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库条目", "id", itemId));

        if (request.getCategoryId() != null) {
            KnowledgeCategory category = knowledgeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", "id", request.getCategoryId()));
            item.setCategory(category);
        } else {
            item.setCategory(null);
        }

        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }

        if (request.getCustomTags() != null) {
            item.setCustomTags(request.getCustomTags());
        }

        KnowledgeItem saved = knowledgeItemRepository.save(item);
        return toItemDto(saved);
    }

    @Transactional
    public void deleteKnowledgeItem(Long userId, Long itemId) {
        KnowledgeItem item = knowledgeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库条目", "id", itemId));
        knowledgeItemRepository.delete(item);
    }

    @Transactional
    public KnowledgeItemDto updateReadStatus(Long userId, Long itemId, String status) {
        KnowledgeItem item = knowledgeItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("知识库条目", "id", itemId));

        try {
            item.setReadStatus(KnowledgeItem.ReadStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("无效的阅读状态: " + status);
        }

        return toItemDto(knowledgeItemRepository.save(item));
    }

    // ==================== 分类管理 ====================

    @Transactional
    public KnowledgeCategoryDto createCategory(Long userId, String name, Long parentId, String icon, String color) {
        if (knowledgeCategoryRepository.existsByUserIdAndName(userId, name)) {
            throw new BadRequestException("分类名称已存在");
        }

        User user = userService.getUserById(userId);
        KnowledgeCategory category = new KnowledgeCategory();
        category.setUser(user);
        category.setName(name);
        category.setIcon(icon);
        category.setColor(color);

        if (parentId != null) {
            KnowledgeCategory parent = knowledgeCategoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("父分类", "id", parentId));
            category.setParent(parent);
        }

        return toCategoryDto(knowledgeCategoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeCategoryDto> getCategories(Long userId) {
        List<KnowledgeCategory> categories = knowledgeCategoryRepository.findByUserIdOrderBySortOrderAsc(userId);
        return buildCategoryTree(categories);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        KnowledgeCategory category = knowledgeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("分类", "id", categoryId));

        if (!category.getUser().getId().equals(userId)) {
            throw new BadRequestException("无权删除此分类");
        }

        knowledgeCategoryRepository.delete(category);
    }

    // ==================== 标签管理 ====================

    @Transactional(readOnly = true)
    public List<String> getAllTags(Long userId) {
        return knowledgeItemRepository.findAllTagsByUserId(userId);
    }

    // ==================== 统计 ====================

    @Transactional(readOnly = true)
    public long getKnowledgeItemCount(Long userId) {
        return knowledgeItemRepository.countByUserId(userId);
    }

    // ==================== 辅助方法 ====================

    private KnowledgeItemDto toItemDto(KnowledgeItem item) {
        return new KnowledgeItemDto(
                item.getId(),
                item.getCachedTitle(),
                item.getCachedContent(),
                item.getCachedSummary(),
                item.getCachedCover(),
                parseTags(item.getCachedTags()),
                item.getCachedAuthorName(),
                item.getCachedAuthorAvatar(),
                item.getOriginalArticleId(),
                item.getOriginalDeleted(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getNotes(),
                item.getReadStatus() != null ? item.getReadStatus().name() : "UNREAD",
                item.getCustomTags(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private KnowledgeCategoryDto toCategoryDto(KnowledgeCategory category) {
        return new KnowledgeCategoryDto(
                category.getId(),
                category.getName(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getIcon(),
                category.getColor(),
                category.getSortOrder(),
                null,
                category.getCreatedAt()
        );
    }

    private List<KnowledgeCategoryDto> buildCategoryTree(List<KnowledgeCategory> categories) {
        List<KnowledgeCategoryDto> rootCategories = new ArrayList<>();

        for (KnowledgeCategory category : categories) {
            if (category.getParent() == null) {
                rootCategories.add(buildCategoryWithChildren(category, categories));
            }
        }

        return rootCategories;
    }

    private KnowledgeCategoryDto buildCategoryWithChildren(KnowledgeCategory category, List<KnowledgeCategory> allCategories) {
        KnowledgeCategoryDto dto = toCategoryDto(category);

        List<KnowledgeCategoryDto> children = new ArrayList<>();
        for (KnowledgeCategory child : allCategories) {
            if (child.getParent() != null && child.getParent().getId().equals(category.getId())) {
                children.add(buildCategoryWithChildren(child, allCategories));
            }
        }

        if (!children.isEmpty()) {
            dto.setChildren(children);
        }

        return dto;
    }

    @SneakyThrows
    private String convertTagsToJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        return objectMapper.writeValueAsString(tags);
    }

    @SneakyThrows
    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    }
}
