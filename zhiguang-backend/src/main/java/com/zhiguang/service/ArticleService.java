package com.zhiguang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zhiguang.dto.ArticleRequestDto;
import com.zhiguang.dto.ArticleResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.Article;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.ArticleFavoriteRepository;
import com.zhiguang.repository.ArticleLikeRepository;
import com.zhiguang.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final Duration ARTICLE_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration HOME_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration TAG_CLOUD_CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_HOT_ARTICLES = 20;
    private static final int LIST_SUMMARY_LENGTH = 120;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "viewCount", "likeCount", "favoriteCount", "commentCount"
    );

    private final ArticleRepository articleRepository;
    private final ArticleFavoriteRepository articleFavoriteRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final UserService userService;
    private final LikeService likeService;
    private final AiService aiService;
    private final ContentModerationService contentModerationService;
    private final FollowService followService;
    private final CacheService cacheService;
    private final ArticleVersionService articleVersionService;
    private final ArticleEmbeddingService articleEmbeddingService;

    @Transactional
    public ArticleResponseDto createArticle(Long userId, ArticleRequestDto request) {
        contentModerationService.validate(request.getTitle());
        contentModerationService.validate(request.getContent());

        User author = userService.getUserById(userId);

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setSummary(request.getSummary());
        article.setCover(request.getCover());
        article.setTags(request.getTags());
        article.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        article.setAuthor(author);

        article = articleRepository.save(article);
        evictHomeCache();
        if (article.getStatus() != null && article.getStatus() == 2) {
            articleEmbeddingService.buildEmbeddingAsync(article);
        }
        return toResponseDto(article, userId);
    }

    @Transactional
    public ArticleResponseDto updateArticle(Long articleId, Long userId, ArticleRequestDto request) {
        Article article = getArticleEntityById(articleId);

        if (!article.getAuthor().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权修改此文章");
        }

        boolean titleChanged = request.getTitle() != null && !Objects.equals(article.getTitle(), request.getTitle());
        boolean contentChanged = request.getContent() != null && !Objects.equals(article.getContent(), request.getContent());
        if (titleChanged || contentChanged) {
            articleVersionService.saveVersion(article);
        }

        if (request.getTitle() != null) {
            contentModerationService.validate(request.getTitle());
            article.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            contentModerationService.validate(request.getContent());
            article.setContent(request.getContent());
        }
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        if (request.getCover() != null) {
            article.setCover(request.getCover());
        }
        if (request.getTags() != null) {
            article.setTags(request.getTags());
        }
        if (request.getStatus() != null) {
            // 状态白名单：作者只能在 1(草稿)/2(已发布) 之间切换；
            // 管理端下架等状态只允许管理员操作，防止作者自行"复活"被处置的文章
            Integer currentStatus = article.getStatus();
            boolean currentIsAuthorManaged = currentStatus == null || currentStatus == 1 || currentStatus == 2;
            boolean targetIsAuthorManaged = request.getStatus() == 1 || request.getStatus() == 2;
            if (!currentIsAuthorManaged || !targetIsAuthorManaged) {
                throw new BadRequestException("当前文章状态不允许此操作");
            }
            article.setStatus(request.getStatus());
        }

        article = articleRepository.save(article);
        evictArticleCache(articleId);
        evictHomeCache();
        if (article.getStatus() != null && article.getStatus() == 2) {
            articleEmbeddingService.buildEmbeddingAsync(article);
        } else {
            articleEmbeddingService.deleteEmbedding(articleId);
        }
        return toResponseDto(article, userId);
    }

    public ArticleResponseDto getArticleById(Long id) {
        return getArticleById(id, null);
    }

    /**
     * 批量获取已发布文章（列表视图）。
     * 与 getArticleById 不同：无浏览计数、无缓存回填等副作用，
     * 供搜索结果/相关推荐做批量映射，避免逐条 3+ 次 DB 点查的 N+1 放大。
     */
    public List<ArticleResponseDto> getPublishedArticlesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Article> byId = articleRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .filter(a -> a.getStatus() != null && a.getStatus() == 2)
                .map(this::toListItemDto)
                .collect(Collectors.toList());
    }

    public ArticleResponseDto getArticleById(Long id, User viewer) {
        // 单条投影查询同时取 status 与 authorId（此前为两次点查）
        Integer status = null;
        Long authorId = null;
        List<Object[]> basic = articleRepository.findStatusAndAuthorById(id);
        if (!basic.isEmpty()) {
            Object[] row = basic.get(0);
            status = (Integer) row[0];
            authorId = (Long) row[1];
        }
        if (status == null || authorId == null) {
            throw new ResourceNotFoundException("文章", "id", id);
        }

        Long viewerId = viewer == null ? null : viewer.getId();
        boolean isAuthor = viewerId != null && viewerId.equals(authorId);
        boolean isAdmin = viewer != null && "ADMIN".equalsIgnoreCase(viewer.getRole());
        if (status != 2 && !isAuthor && !isAdmin) {
            throw new ResourceNotFoundException("文章", "id", id);
        }

        ArticleResponseDto dto;
        if (status == 2) {
            dto = cacheService.getOrLoad(
                    "article:" + id,
                    ARTICLE_CACHE_TTL,
                    () -> Optional.ofNullable(loadArticleFromDb(id)),
                    ArticleResponseDto.class
            ).orElseThrow(() -> new ResourceNotFoundException("文章", "id", id));
        } else {
            Article article = articleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("文章", "id", id));
            dto = toResponseDto(article, null);
        }

        long liveViewCount = Optional.ofNullable(articleRepository.findViewCountById(id)).orElse(0);
        long pendingViewCount = (isAuthor || isAdmin)
                ? cacheService.getPendingViewCount(id)
                : cacheService.incrementView(id);
        long viewCount = liveViewCount + pendingViewCount;
        dto.setViewCount((int) Math.min(viewCount, Integer.MAX_VALUE));

        if (viewerId != null) {
            dto.setIsLiked(likeService.isLiked(id, viewerId));
            dto.setIsFavorited(likeService.isFavorited(id, viewerId));
        }

        return dto;
    }

    private ArticleResponseDto loadArticleFromDb(Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            return null;
        }
        return toResponseDto(article, null);
    }

    public Article getArticleEntityById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", id));
    }

    public PageResponse<ArticleResponseDto> getArticles(int page, int size, String sortBy) {
        String safeSortBy = sanitizeSortBy(sortBy);
        String cacheKey = "home:articles:" + page + ":" + size + ":" + safeSortBy;
        return cacheService.getOrLoad(
                cacheKey,
                HOME_CACHE_TTL,
                () -> Optional.of(loadArticlesFromDb(page, size, safeSortBy)),
                new TypeReference<PageResponse<ArticleResponseDto>>() {}
        ).orElseGet(() -> new PageResponse<>(List.of(), 0, page, size, 0));
    }

    private PageResponse<ArticleResponseDto> loadArticlesFromDb(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<Article> articlePage = articleRepository.findByStatus(2, pageable);
        return toPageResponse(articlePage);
    }

    public PageResponse<ArticleResponseDto> getArticlesByAuthor(Long authorId, User viewer, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean canViewDrafts = viewer != null && (viewer.getId().equals(authorId) || "ADMIN".equalsIgnoreCase(viewer.getRole()));
        Page<Article> articlePage = canViewDrafts
                ? articleRepository.findByAuthorId(authorId, pageable)
                : articleRepository.findByAuthorIdAndStatus(authorId, 2, pageable);
        return decorateListPage(toPageResponse(articlePage), viewer);
    }

    public PageResponse<ArticleResponseDto> searchArticles(String keyword, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sanitizeSortBy(sortBy)));

        if (keyword != null && keyword.trim().length() >= 3) {
            try {
                Page<Long> idPage = articleRepository.findIdsByFullText(keyword.trim(), PageRequest.of(page, size));
                if (idPage.hasContent()) {
                    List<Long> ids = idPage.getContent();
                    Map<Long, Article> articleMap = articleRepository.findByIdIn(ids).stream()
                            .collect(Collectors.toMap(Article::getId, Function.identity()));
                    List<ArticleResponseDto> ordered = ids.stream()
                            .map(articleMap::get)
                            .filter(Objects::nonNull)
                            .map(this::toListItemDto)
                            .collect(Collectors.toList());
                    return new PageResponse<>(ordered, idPage.getTotalElements(), idPage.getNumber(), idPage.getSize(), idPage.getTotalPages());
                }
            } catch (Exception e) {
                log.warn("全文搜索失败，降级为 LIKE 查询: keyword={}", keyword, e);
            }
        }

        Page<Article> articlePage = articleRepository.searchByKeyword(keyword, pageable);
        return toPageResponse(articlePage);
    }

    public PageResponse<ArticleResponseDto> getArticlesByTags(List<String> tags, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sanitizeSortBy(sortBy)));
        Page<Article> articlePage = articleRepository.findByTagsIn(tags, pageable);
        return toPageResponse(articlePage);
    }

    public PageResponse<ArticleResponseDto> getUserFavorites(Long userId, int page, int size) {
        Page<Article> favoritePage = articleFavoriteRepository.findPublishedFavorites(
                userId,
                PageRequest.of(page, size)
        );
        PageResponse<ArticleResponseDto> response = toPageResponse(favoritePage);
        Set<Long> likedIds = batchLikedIds(userId, response);
        response.getList().forEach(dto -> {
            dto.setIsLiked(likedIds.contains(dto.getId()));
            dto.setIsFavorited(true);
        });
        return response;
    }

    public List<Map<String, Object>> getTagCloud() {
        return cacheService.getOrLoad(
                "home:tag-cloud",
                TAG_CLOUD_CACHE_TTL,
                () -> Optional.of(loadTagCloud()),
                new TypeReference<List<Map<String, Object>>>() {}
        ).orElseGet(List::of);
    }

    private List<Map<String, Object>> loadTagCloud() {
        return articleRepository.findTagStatistics().stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("tag", row[0]);
                    map.put("count", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    public PageResponse<ArticleResponseDto> getFollowingFeed(Long userId, int page, int size) {
        List<Long> followingIds = followService.getFollowingIds(userId);
        if (followingIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, page, size, 0);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articlePage = articleRepository.findByAuthorIdIn(followingIds, pageable);
        PageResponse<ArticleResponseDto> response = toPageResponse(articlePage);

        Set<Long> likedIds = batchLikedIds(userId, response);
        Set<Long> favoritedIds = batchFavoritedIds(userId, response);
        response.getList().forEach(dto -> {
            dto.setIsLiked(likedIds.contains(dto.getId()));
            dto.setIsFavorited(favoritedIds.contains(dto.getId()));
        });
        return response;
    }

    @Transactional
    public void deleteArticle(Long articleId, Long userId) {
        Article article = getArticleEntityById(articleId);
        if (!article.getAuthor().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权删除此文章");
        }

        articleRepository.delete(article);
        evictArticleCache(articleId);
        evictHomeCache();
        articleEmbeddingService.deleteEmbedding(articleId);
    }

    public List<ArticleResponseDto> getHotArticles(int limit) {
        int safeLimit = clampLimit(limit);
        String cacheKey = "home:hot:" + safeLimit;
        return cacheService.getOrLoad(
                cacheKey,
                HOME_CACHE_TTL,
                () -> Optional.of(loadHotArticles(safeLimit)),
                new TypeReference<List<ArticleResponseDto>>() {}
        ).orElseGet(List::of);
    }

    private List<ArticleResponseDto> loadHotArticles(int limit) {
        return articleRepository.findTopByViewCount(PageRequest.of(0, limit))
                .stream()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
    }

    public List<ArticleResponseDto> getPopularArticles(int limit) {
        int safeLimit = clampLimit(limit);
        String cacheKey = "home:popular:" + safeLimit;
        return cacheService.getOrLoad(
                cacheKey,
                HOME_CACHE_TTL,
                () -> Optional.of(loadPopularArticles(safeLimit)),
                new TypeReference<List<ArticleResponseDto>>() {}
        ).orElseGet(List::of);
    }

    private List<ArticleResponseDto> loadPopularArticles(int limit) {
        return articleRepository.findTopByLikeCount(PageRequest.of(0, limit))
                .stream()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public String summarizeArticle(Long articleId, boolean force) {
        Article article = getArticleEntityById(articleId);
        if (!force && article.getAiSummary() != null && !article.getAiSummary().isBlank()) {
            return article.getAiSummary();
        }

        String summary = aiService.summarizeContent(article.getTitle(), article.getContent());
        article.setAiSummary(summary);
        articleRepository.save(article);
        evictArticleCache(articleId);
        return summary;
    }

    private PageResponse<ArticleResponseDto> toPageResponse(Page<Article> articlePage) {
        List<ArticleResponseDto> list = articlePage.getContent().stream()
                .map(this::toListItemDto)
                .collect(Collectors.toList());
        return new PageResponse<>(
                list,
                articlePage.getTotalElements(),
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalPages()
        );
    }

    private PageResponse<ArticleResponseDto> decorateListPage(PageResponse<ArticleResponseDto> response, User viewer) {
        if (viewer == null) {
            return response;
        }
        Set<Long> likedIds = batchLikedIds(viewer.getId(), response);
        Set<Long> favoritedIds = batchFavoritedIds(viewer.getId(), response);
        response.getList().forEach(dto -> {
            dto.setIsLiked(likedIds.contains(dto.getId()));
            dto.setIsFavorited(favoritedIds.contains(dto.getId()));
        });
        return response;
    }

    private Set<Long> batchLikedIds(Long userId, PageResponse<ArticleResponseDto> response) {
        if (userId == null || response.getList().isEmpty()) {
            return Set.of();
        }
        List<Long> ids = response.getList().stream().map(ArticleResponseDto::getId).collect(Collectors.toList());
        return new HashSet<>(articleLikeRepository.findLikedArticleIds(userId, ids));
    }

    private Set<Long> batchFavoritedIds(Long userId, PageResponse<ArticleResponseDto> response) {
        if (userId == null || response.getList().isEmpty()) {
            return Set.of();
        }
        List<Long> ids = response.getList().stream().map(ArticleResponseDto::getId).collect(Collectors.toList());
        return new HashSet<>(articleFavoriteRepository.findFavoritedArticleIds(userId, ids));
    }

    private ArticleResponseDto toResponseDto(Article article, Long userId) {
        ArticleResponseDto dto = buildDto(article, false);
        dto.setContent(article.getContent());
        if (userId != null) {
            dto.setIsLiked(likeService.isLiked(article.getId(), userId));
            dto.setIsFavorited(likeService.isFavorited(article.getId(), userId));
        }
        return dto;
    }

    private ArticleResponseDto toListItemDto(Article article) {
        ArticleResponseDto dto = buildDto(article, true);
        dto.setContent(null);
        return dto;
    }

    private ArticleResponseDto buildDto(Article article, boolean listMode) {
        ArticleResponseDto.AuthorDto authorDto = new ArticleResponseDto.AuthorDto(
                article.getAuthor().getId(),
                article.getAuthor().getUsername(),
                article.getAuthor().getAvatar(),
                article.getAuthor().getBio()
        );

        String summary = article.getSummary();
        if (listMode && (summary == null || summary.isBlank())) {
            summary = buildSummaryFallback(article.getContent());
        }

        return new ArticleResponseDto(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                summary,
                article.getCover(),
                authorDto,
                article.getStatus(),
                article.getViewCount(),
                article.getLikeCount(),
                article.getFavoriteCount(),
                article.getCommentCount(),
                article.getTags(),
                false,
                false,
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getAiSummary()
        );
    }

    private String buildSummaryFallback(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String plainText = content.replaceAll("[#*\\[\\]`]", "").replaceAll("\\s+", " ").trim();
        if (plainText.length() <= LIST_SUMMARY_LENGTH) {
            return plainText;
        }
        return plainText.substring(0, LIST_SUMMARY_LENGTH) + "...";
    }

    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return "createdAt";
        }
        return sortBy;
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_HOT_ARTICLES));
    }

    private void evictArticleCache(Long articleId) {
        cacheService.evict("article:" + articleId);
    }

    private void evictHomeCache() {
        cacheService.evictByPrefix("home:");
    }
}
