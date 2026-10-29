package com.zhiguang.service;

import com.zhiguang.dto.*;
import com.zhiguang.entity.*;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionArticleRepository collectionArticleRepository;
    private final ArticleService articleService;

    @Transactional
    public CollectionResponseDto create(Long userId, CollectionRequestDto request, User author) {
        ArticleCollection entity = new ArticleCollection();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setUser(author);
        entity = collectionRepository.save(entity);
        return toResponseDto(entity);
    }

    @Transactional
    public CollectionResponseDto update(Long collectionId, Long userId, CollectionRequestDto request) {
        ArticleCollection entity = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("合集", "id", collectionId));
        if (!entity.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权修改此合集");
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        entity = collectionRepository.save(entity);
        return toResponseDto(entity);
    }

    public CollectionResponseDto getById(Long collectionId) {
        ArticleCollection entity = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("合集", "id", collectionId));
        return toResponseDto(entity);
    }

    public PageResponse<CollectionResponseDto> getUserCollections(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ArticleCollection> entityPage = collectionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<CollectionResponseDto> list = entityPage.getContent().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return new PageResponse<>(list, entityPage.getTotalElements(), page, size, entityPage.getTotalPages());
    }

    /** 公开：全部合集 */
    public PageResponse<CollectionResponseDto> getAllCollections(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ArticleCollection> entityPage = collectionRepository.findAll(pageable);
        List<CollectionResponseDto> list = entityPage.getContent().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return new PageResponse<>(list, entityPage.getTotalElements(), page, size, entityPage.getTotalPages());
    }

    @Transactional
    public void addArticle(Long collectionId, Long articleId, Long userId) {
        ArticleCollection entity = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("合集", "id", collectionId));
        if (!entity.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权操作此合集");
        }
        if (collectionArticleRepository.existsByCollectionIdAndArticleId(collectionId, articleId)) {
            return;
        }
        CollectionArticle ca = new CollectionArticle();
        ca.setCollectionId(collectionId);
        ca.setArticleId(articleId);
        int count = collectionArticleRepository.countByCollectionId(collectionId);
        ca.setSortOrder(count);
        collectionArticleRepository.save(ca);
        entity.setArticleCount(count + 1);
        collectionRepository.save(entity);
    }

    @Transactional
    public void removeArticle(Long collectionId, Long articleId, Long userId) {
        ArticleCollection entity = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("合集", "id", collectionId));
        if (!entity.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权操作此合集");
        }
        collectionArticleRepository.deleteByCollectionIdAndArticleId(collectionId, articleId);
        int count = collectionArticleRepository.countByCollectionId(collectionId);
        entity.setArticleCount(count);
        collectionRepository.save(entity);
    }

    public PageResponse<ArticleResponseDto> getArticles(Long collectionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder"));
        Page<CollectionArticle> caPage = collectionArticleRepository.findByCollectionIdOrderBySortOrderAsc(collectionId, pageable);
        List<ArticleResponseDto> list = caPage.getContent().stream()
                .map(ca -> articleService.getArticleById(ca.getArticleId()))
                .collect(Collectors.toList());
        return new PageResponse<>(list, caPage.getTotalElements(), page, size, caPage.getTotalPages());
    }

    @Transactional
    public void delete(Long collectionId, Long userId) {
        ArticleCollection entity = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("合集", "id", collectionId));
        if (!entity.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权删除此合集");
        }
        collectionRepository.delete(entity);
    }

    private CollectionResponseDto toResponseDto(ArticleCollection entity) {
        ColumnResponseDto.UserBriefDto author = new ColumnResponseDto.UserBriefDto(
                entity.getUser().getId(), entity.getUser().getUsername(), entity.getUser().getAvatar());
        return new CollectionResponseDto(
                entity.getId(), entity.getName(), entity.getDescription(),
                author, entity.getArticleCount(), entity.getCreatedAt());
    }
}
