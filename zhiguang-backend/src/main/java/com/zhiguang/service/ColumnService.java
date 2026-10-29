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
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final ColumnArticleRepository columnArticleRepository;
    private final ColumnSubscriptionRepository subscriptionRepository;
    private final ArticleService articleService;
    private final UserService userService;

    /** 创建专栏 */
    @Transactional
    public ColumnResponseDto createColumn(Long userId, ColumnRequestDto request, User author) {
        ColumnEntity column = new ColumnEntity();
        column.setName(request.getName());
        column.setDescription(request.getDescription());
        column.setCover(request.getCover());
        column.setUser(author);
        column = columnRepository.save(column);
        return toResponseDto(column, userId);
    }

    /** 编辑专栏 */
    @Transactional
    public ColumnResponseDto updateColumn(Long columnId, Long userId, ColumnRequestDto request) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        if (!column.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权修改此专栏");
        }
        if (request.getName() != null) column.setName(request.getName());
        if (request.getDescription() != null) column.setDescription(request.getDescription());
        if (request.getCover() != null) column.setCover(request.getCover());
        column = columnRepository.save(column);
        return toResponseDto(column, userId);
    }

    /** 获取专栏详情 */
    public ColumnResponseDto getColumnById(Long columnId, Long userId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        return toResponseDto(column, userId);
    }

    /** 分页获取用户创建的专栏 */
    public PageResponse<ColumnResponseDto> getUserColumns(Long userId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ColumnEntity> columnPage = columnRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<ColumnResponseDto> list = columnPage.getContent().stream()
                .map(c -> toResponseDto(c, currentUserId))
                .collect(Collectors.toList());
        return new PageResponse<>(list, columnPage.getTotalElements(), page, size, columnPage.getTotalPages());
    }

    /** 公开：全部专栏（按订阅数排序） */
    public PageResponse<ColumnResponseDto> getAllColumns(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "subscriberCount"));
        Page<ColumnEntity> columnPage = columnRepository.findAll(pageable);
        List<ColumnResponseDto> list = columnPage.getContent().stream()
                .map(c -> toResponseDto(c, currentUserId))
                .collect(Collectors.toList());
        return new PageResponse<>(list, columnPage.getTotalElements(), page, size, columnPage.getTotalPages());
    }

    /** 添加文章到专栏 */
    @Transactional
    public void addArticleToColumn(Long columnId, Long articleId, Long userId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        if (!column.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权操作此专栏");
        }
        if (columnArticleRepository.existsByColumnIdAndArticleId(columnId, articleId)) {
            return;
        }
        ColumnArticle ca = new ColumnArticle();
        ca.setColumnId(columnId);
        ca.setArticleId(articleId);
        int count = columnArticleRepository.countByColumnId(columnId);
        ca.setSortOrder(count);
        columnArticleRepository.save(ca);
    }

    /** 从专栏移除文章 */
    @Transactional
    public void removeArticleFromColumn(Long columnId, Long articleId, Long userId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        if (!column.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权操作此专栏");
        }
        columnArticleRepository.deleteByColumnIdAndArticleId(columnId, articleId);
    }

    /** 获取专栏内的文章 */
    public PageResponse<ArticleResponseDto> getColumnArticles(Long columnId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sortOrder"));
        Page<ColumnArticle> caPage = columnArticleRepository.findByColumnIdOrderBySortOrderAsc(columnId, pageable);
        List<ArticleResponseDto> list = caPage.getContent().stream()
                .map(ca -> articleService.getArticleById(ca.getArticleId()))
                .collect(Collectors.toList());
        return new PageResponse<>(list, caPage.getTotalElements(), page, size, caPage.getTotalPages());
    }

    /** 订阅专栏 */
    @Transactional
    public void subscribe(Long columnId, Long userId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        if (subscriptionRepository.existsByUserIdAndColumnId(userId, columnId)) {
            return;
        }
        ColumnSubscription sub = new ColumnSubscription();
        sub.setColumn(column);
        sub.setUser(userService.getUserById(userId));
        subscriptionRepository.save(sub);
        column.setSubscriberCount(column.getSubscriberCount() + 1);
        columnRepository.save(column);
    }

    /** 取消订阅 */
    @Transactional
    public void unsubscribe(Long columnId, Long userId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        if (!subscriptionRepository.existsByUserIdAndColumnId(userId, columnId)) {
            return;
        }
        subscriptionRepository.deleteByUserIdAndColumnId(userId, columnId);
        column.setSubscriberCount(Math.max(0, column.getSubscriberCount() - 1));
        columnRepository.save(column);
    }

    /** 查询是否订阅 */
    public boolean isSubscribed(Long columnId, Long userId) {
        return userId != null && subscriptionRepository.existsByUserIdAndColumnId(userId, columnId);
    }

    /** 删除专栏 */
    @Transactional
    public void deleteColumn(Long columnId, Long userId) {
        ColumnEntity column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("专栏", "id", columnId));
        if (!column.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权删除此专栏");
        }
        columnRepository.delete(column);
    }

    private ColumnResponseDto toResponseDto(ColumnEntity column, Long userId) {
        ColumnResponseDto.UserBriefDto author = new ColumnResponseDto.UserBriefDto(
                column.getUser().getId(),
                column.getUser().getUsername(),
                column.getUser().getAvatar()
        );
        int articleCount = columnArticleRepository.countByColumnId(column.getId());
        boolean subscribed = userId != null && subscriptionRepository.existsByUserIdAndColumnId(userId, column.getId());
        return new ColumnResponseDto(
                column.getId(), column.getName(), column.getDescription(), column.getCover(),
                author, column.getSubscriberCount(), articleCount, subscribed, column.getCreatedAt()
        );
    }
}
