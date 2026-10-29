package com.zhiguang.service;

import com.zhiguang.dto.CommentRequestDto;
import com.zhiguang.dto.CommentResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.Article;
import com.zhiguang.entity.Comment;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.CommentRepository;
import com.zhiguang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]+)");
    private static final int MAX_PAGE_SIZE = 100;

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;
    private final MessageQueueService messageQueueService;
    private final CacheService cacheService;
    private final UserGrowthService userGrowthService;
    private final TransactionTemplate transactionTemplate;

    public CommentResponseDto createComment(Long articleId, User viewer, CommentRequestDto request) {
        if (viewer == null) {
            throw new BadRequestException("请先登录");
        }

        contentModerationService.validate(request.getContent());
        Article article = requireAccessibleArticle(articleId, viewer);

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("评论", "id", request.getParentId()));
            if (!parent.getArticle().getId().equals(articleId)) {
                throw new BadRequestException("父评论不属于当前文章");
            }
        }

        final Comment finalParent = parent;
        CommentResponseDto created = transactionTemplate.execute(status -> {
            User commenter = userRepository.findById(viewer.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            Comment entity = new Comment();
            entity.setContent(request.getContent());
            entity.setArticle(article);
            entity.setUser(commenter);
            entity.setParent(finalParent);

            Comment saved = commentRepository.save(entity);
            articleRepository.syncCommentCount(articleId);
            return toResponseDto(saved, List.of());
        });

        cacheService.evict("article:" + articleId);

        Long authorId = article.getAuthor().getId();
        if (!authorId.equals(viewer.getId())) {
            sendAsyncNotification(authorId, "COMMENT",
                    viewer.getUsername() + " 评论了你的文章《" + article.getTitle() + "》", articleId);
        }

        try {
            userGrowthService.addExperience(viewer.getId(), "CREATE_COMMENT", 5);
            userGrowthService.checkAchievements(viewer.getId());
        } catch (Exception e) {
            log.warn("评论成长奖励处理失败: userId={}", viewer.getId(), e);
        }

        sendMentionNotifications(article, viewer, request.getContent());
        return created;
    }

    public PageResponse<CommentResponseDto> getCommentsByArticleId(Long articleId, User viewer, int page, int size) {
        requireAccessibleArticle(articleId, viewer);

        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByArticleIdAndParentIsNull(articleId, pageable);

        List<Long> parentIds = commentPage.getContent().stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        Map<Long, List<Comment>> repliesByParentId = parentIds.isEmpty()
                ? Map.of()
                : commentRepository.findRepliesByParentIds(parentIds).stream()
                        .collect(Collectors.groupingBy(reply -> reply.getParent().getId()));

        List<CommentResponseDto> list = commentPage.getContent().stream()
                .map(comment -> toResponseDto(comment, repliesByParentId.getOrDefault(comment.getId(), List.of())))
                .collect(Collectors.toList());

        return new PageResponse<>(
                list,
                commentPage.getTotalElements(),
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalPages()
        );
    }

    public void deleteComment(Long commentId, User viewer) {
        if (viewer == null) {
            throw new BadRequestException("请先登录");
        }

        Long articleId = transactionTemplate.execute(status -> {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new ResourceNotFoundException("评论", "id", commentId));

            boolean isCommentAuthor = comment.getUser().getId().equals(viewer.getId());
            boolean isArticleAuthor = comment.getArticle().getAuthor().getId().equals(viewer.getId());
            boolean isAdmin = "ADMIN".equalsIgnoreCase(viewer.getRole());

            if (!isCommentAuthor && !isArticleAuthor && !isAdmin) {
                throw new ResourceNotFoundException("无权删除此评论");
            }

            commentRepository.delete(comment);
            articleRepository.syncCommentCount(comment.getArticle().getId());
            return comment.getArticle().getId();
        });

        if (articleId != null) {
            cacheService.evict("article:" + articleId);
        }
    }

    private Article requireAccessibleArticle(Long articleId, User viewer) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", articleId));

        Integer status = article.getStatus();
        boolean isAuthor = viewer != null && article.getAuthor().getId().equals(viewer.getId());
        boolean isAdmin = viewer != null && "ADMIN".equalsIgnoreCase(viewer.getRole());
        if ((status == null || status != 2) && !isAuthor && !isAdmin) {
            throw new ResourceNotFoundException("文章", "id", articleId);
        }
        return article;
    }

    private CommentResponseDto toResponseDto(Comment comment, List<Comment> replies) {
        CommentResponseDto.ArticleBriefDto articleDto = null;
        if (comment.getArticle() != null) {
            articleDto = new CommentResponseDto.ArticleBriefDto(
                    comment.getArticle().getId(),
                    comment.getArticle().getTitle()
            );
        }

        CommentResponseDto.UserBriefDto userDto = new CommentResponseDto.UserBriefDto(
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getAvatar()
        );

        CommentResponseDto.UserBriefDto parentDto = null;
        if (comment.getParent() != null) {
            parentDto = new CommentResponseDto.UserBriefDto(
                    comment.getParent().getUser().getId(),
                    comment.getParent().getUser().getUsername(),
                    comment.getParent().getUser().getAvatar()
            );
        }

        List<CommentResponseDto> replyDtos = replies == null ? List.of() : replies.stream()
                .map(reply -> toResponseDto(reply, List.of()))
                .collect(Collectors.toList());

        return new CommentResponseDto(
                comment.getId(),
                comment.getContent(),
                articleDto,
                userDto,
                parentDto,
                comment.getCreatedAt(),
                false,
                0L,
                replyDtos
        );
    }

    private void sendAsyncNotification(Long userId, String type, String content, Long articleId) {
        try {
            messageQueueService.sendNotification(userId, type, content, articleId);
        } catch (Exception e) {
            log.warn("评论异步通知发送失败: userId={}, articleId={}", userId, articleId, e);
        }
    }

    private void sendMentionNotifications(Article article, User viewer, String content) {
        Set<String> notifiedUsers = new java.util.HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (!notifiedUsers.add(username)) {
                continue;
            }

            userRepository.findByUsername(username).ifPresent(mentionedUser -> {
                if (!mentionedUser.getId().equals(viewer.getId())
                        && !mentionedUser.getId().equals(article.getAuthor().getId())) {
                    sendAsyncNotification(mentionedUser.getId(), "MENTION",
                            viewer.getUsername() + " 在文章《" + article.getTitle() + "》的评论中提到了你",
                            article.getId());
                }
            });
        }
    }
}
