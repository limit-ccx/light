package com.zhiguang.repository;

import com.zhiguang.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Article> findByStatus(Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Article> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Article> findByAuthorIdAndStatus(Long authorId, Integer status, Pageable pageable);

    long countByAuthorIdAndStatus(Long authorId, Integer status);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM Article a WHERE a.status = 2 AND (a.title LIKE %:keyword% OR a.content LIKE %:keyword%)")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT a.id FROM articles a WHERE a.status = 2 AND MATCH(a.title, a.content) AGAINST (:keyword IN NATURAL LANGUAGE MODE) ORDER BY a.created_at DESC",
            countQuery = "SELECT COUNT(a.id) FROM articles a WHERE a.status = 2 AND MATCH(a.title, a.content) AGAINST (:keyword IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    Page<Long> findIdsByFullText(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM Article a WHERE a.id IN :ids")
    List<Article> findByIdIn(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"author"})
    @Query(value = "SELECT a FROM Article a WHERE a.status = 2 AND EXISTS (SELECT t FROM a.tags t WHERE t IN :tags)",
            countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = 2 AND EXISTS (SELECT t FROM a.tags t WHERE t IN :tags)")
    Page<Article> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM Article a WHERE a.status = 2 ORDER BY a.viewCount DESC")
    List<Article> findTopByViewCount(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM Article a WHERE a.status = 2 ORDER BY a.likeCount DESC")
    List<Article> findTopByLikeCount(Pageable pageable);

    @Query("SELECT t, COUNT(a) FROM Article a JOIN a.tags t WHERE a.status = 2 GROUP BY t ORDER BY COUNT(a) DESC")
    List<Object[]> findTagStatistics();

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM Article a WHERE a.status = 2 AND a.author.id IN :authorIds ORDER BY a.createdAt DESC")
    Page<Article> findByAuthorIdIn(@Param("authorIds") List<Long> authorIds, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Optional<Article> findById(Long id);

    long countByAuthorId(Long authorId);

    @Query("SELECT COALESCE(SUM(a.likeCount), 0) FROM Article a WHERE a.author.id = :userId")
    Integer sumLikeCountByAuthorId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.favoriteCount), 0) FROM Article a WHERE a.author.id = :userId")
    Integer sumFavoriteCountByAuthorId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.viewCount), 0) FROM Article a WHERE a.author.id = :userId")
    Integer sumViewCountByAuthorId(@Param("userId") Long userId);

    @Query("SELECT a.status FROM Article a WHERE a.id = :id")
    Integer findStatusById(@Param("id") Long id);

    /** 详情接口的可见性预检：一次查出 status 与 authorId（替代两次点查，PERF-P2-3） */
    @Query("SELECT a.status, a.author.id FROM Article a WHERE a.id = :id")
    List<Object[]> findStatusAndAuthorById(@Param("id") Long id);

    @Query("SELECT a.viewCount FROM Article a WHERE a.id = :id")
    Integer findViewCountById(@Param("id") Long id);

    @Query("SELECT a.likeCount FROM Article a WHERE a.id = :id")
    Integer findLikeCountById(@Param("id") Long id);

    @Query("SELECT a.favoriteCount FROM Article a WHERE a.id = :id")
    Integer findFavoriteCountById(@Param("id") Long id);

    @Query("SELECT a.commentCount FROM Article a WHERE a.id = :id")
    Integer findCommentCountById(@Param("id") Long id);

    @Query("SELECT a.author.id FROM Article a WHERE a.id = :id")
    Long findAuthorIdById(@Param("id") Long id);

    @Query("SELECT a.title FROM Article a WHERE a.id = :id")
    String findTitleById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE articles SET view_count = COALESCE(view_count, 0) + :delta, updated_at = updated_at WHERE id = :id",
            nativeQuery = true)
    int increaseViewCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE articles SET like_count = (SELECT COUNT(*) FROM article_likes WHERE article_id = :articleId), updated_at = updated_at WHERE id = :articleId",
            nativeQuery = true)
    int syncLikeCount(@Param("articleId") Long articleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE articles SET favorite_count = (SELECT COUNT(*) FROM article_favorites WHERE article_id = :articleId), updated_at = updated_at WHERE id = :articleId",
            nativeQuery = true)
    int syncFavoriteCount(@Param("articleId") Long articleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE articles SET comment_count = (SELECT COUNT(*) FROM comments WHERE article_id = :articleId), updated_at = updated_at WHERE id = :articleId",
            nativeQuery = true)
    int syncCommentCount(@Param("articleId") Long articleId);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT a FROM Article a WHERE " +
            "(:status IS NULL OR a.status = :status) " +
            "AND (:keyword IS NULL OR a.title LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY a.createdAt DESC")
    Page<Article> searchForAdmin(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Article a WHERE DATE(a.createdAt) = CURRENT_DATE")
    long countByCreatedToday();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    long countByStatus(@Param("status") Integer status);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.createdAt >= :start AND a.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(a.likeCount), 0) FROM Article a")
    long sumAllLikeCount();

    @Query("SELECT a.id FROM Article a")
    List<Long> findAllArticleIds();
}
