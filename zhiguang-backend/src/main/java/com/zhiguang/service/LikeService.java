package com.zhiguang.service;

import com.zhiguang.entity.Article;
import com.zhiguang.entity.ArticleFavorite;
import com.zhiguang.entity.ArticleLike;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.ArticleFavoriteRepository;
import com.zhiguang.repository.ArticleLikeRepository;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final ArticleLikeRepository articleLikeRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final MessageQueueService messageQueueService;
    private final CacheService cacheService;
    private final UserGrowthService userGrowthService;
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

    public Map<String, Object> toggleLike(Long articleId, User viewer) {
        requireLoggedIn(viewer);
        ArticleAccess access = requireAccessibleArticle(articleId, viewer);

        String lockKey = "lock:like:" + articleId + ":" + viewer.getId();
        ToggleResult result = distributedLockService.executeWithLock(lockKey, 3, 10, () ->
                transactionTemplate.execute(status -> {
                    boolean exists = articleLikeRepository.existsByArticleIdAndUserId(articleId, viewer.getId());
                    if (exists) {
                        articleLikeRepository.deleteByArticleIdAndUserId(articleId, viewer.getId());
                    } else {
                        Article article = articleRepository.getReferenceById(articleId);
                        User user = userRepository.findById(viewer.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
                        ArticleLike like = new ArticleLike();
                        like.setArticle(article);
                        like.setUser(user);
                        articleLikeRepository.save(like);
                    }
                    articleRepository.syncLikeCount(articleId);
                    return new ToggleResult(!exists, articleId, viewer.getId(), access.authorId(), access.title(), viewer.getUsername());
                })
        );

        cacheService.evict("article:" + articleId);
        if (result.enabled()) {
            notifyAuthor(result, "LIKE", "点赞");
            grantExperience(result.authorId(), "ARTICLE_LIKED", 2);
        }

        return Map.of("isLiked", result.enabled(), "likeCount", getLikeCount(articleId));
    }

    public Map<String, Object> toggleFavorite(Long articleId, User viewer) {
        requireLoggedIn(viewer);
        ArticleAccess access = requireAccessibleArticle(articleId, viewer);

        String lockKey = "lock:favorite:" + articleId + ":" + viewer.getId();
        ToggleResult result = distributedLockService.executeWithLock(lockKey, 3, 10, () ->
                transactionTemplate.execute(status -> {
                    boolean exists = articleFavoriteRepository.existsByArticleIdAndUserId(articleId, viewer.getId());
                    if (exists) {
                        articleFavoriteRepository.deleteByArticleIdAndUserId(articleId, viewer.getId());
                    } else {
                        Article article = articleRepository.getReferenceById(articleId);
                        User user = userRepository.findById(viewer.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
                        ArticleFavorite favorite = new ArticleFavorite();
                        favorite.setArticle(article);
                        favorite.setUser(user);
                        articleFavoriteRepository.save(favorite);
                    }
                    articleRepository.syncFavoriteCount(articleId);
                    return new ToggleResult(!exists, articleId, viewer.getId(), access.authorId(), access.title(), viewer.getUsername());
                })
        );

        cacheService.evict("article:" + articleId);
        if (result.enabled()) {
            notifyAuthor(result, "FAVORITE", "收藏");
            grantExperience(result.authorId(), "ARTICLE_FAVORITED", 5);
        }

        return Map.of("isFavorited", result.enabled(), "favoriteCount", getFavoriteCount(articleId));
    }

    public boolean isLiked(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return articleLikeRepository.existsByArticleIdAndUserId(articleId, userId);
    }

    public boolean isFavorited(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        return articleFavoriteRepository.existsByArticleIdAndUserId(articleId, userId);
    }

    public long getLikeCount(Long articleId) {
        Integer count = articleRepository.findLikeCountById(articleId);
        return count == null ? 0 : count;
    }

    public long getFavoriteCount(Long articleId) {
        Integer count = articleRepository.findFavoriteCountById(articleId);
        return count == null ? 0 : count;
    }

    public Map<String, Object> getLikeStatus(Long articleId, Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked(articleId, userId));
        result.put("likeCount", getLikeCount(articleId));
        result.put("isFavorited", isFavorited(articleId, userId));
        result.put("favoriteCount", getFavoriteCount(articleId));
        return result;
    }

    private void requireLoggedIn(User viewer) {
        if (viewer == null) {
            throw new BadRequestException("请先登录");
        }
    }

    private ArticleAccess requireAccessibleArticle(Long articleId, User viewer) {
        Integer status = articleRepository.findStatusById(articleId);
        Long authorId = articleRepository.findAuthorIdById(articleId);
        String title = articleRepository.findTitleById(articleId);
        if (status == null || authorId == null) {
            throw new ResourceNotFoundException("文章不存在");
        }
        if (!canAccess(status, authorId, viewer)) {
            throw new ResourceNotFoundException("文章不存在");
        }
        return new ArticleAccess(status, authorId, title == null ? "" : title);
    }

    private boolean canAccess(Integer status, Long authorId, User viewer) {
        return status == 2 || (viewer != null && (viewer.getId().equals(authorId) || "ADMIN".equalsIgnoreCase(viewer.getRole())));
    }

    private void notifyAuthor(ToggleResult result, String type, String action) {
        if (result.authorId().equals(result.userId())) {
            return;
        }
        try {
            String content = result.username() + " " + action + "了你的文章《" + result.title() + "》";
            messageQueueService.sendNotification(result.authorId(), type, content, result.articleId());
        } catch (Exception e) {
            log.warn("异步通知发送失败: type={}, articleId={}, authorId={}", type, result.articleId(), result.authorId(), e);
        }
    }

    private void grantExperience(Long authorId, String source, int points) {
        try {
            userGrowthService.addExperience(authorId, source, points);
            userGrowthService.checkAchievements(authorId);
        } catch (Exception e) {
            log.warn("互动成长奖励处理失败: authorId={}, source={}", authorId, source, e);
        }
    }

    private record ArticleAccess(Integer status, Long authorId, String title) {
    }

    private record ToggleResult(boolean enabled, Long articleId, Long userId, Long authorId, String title, String username) {
    }
}
