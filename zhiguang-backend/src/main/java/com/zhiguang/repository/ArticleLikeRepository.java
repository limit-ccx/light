package com.zhiguang.repository;

import com.zhiguang.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    Optional<ArticleLike> findByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    long countByArticleId(Long articleId);

    @Query("SELECT l.article.id FROM ArticleLike l WHERE l.user.id = :userId AND l.article.id IN :articleIds")
    List<Long> findLikedArticleIds(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);

    @Query("SELECT COUNT(l) FROM ArticleLike l WHERE l.createdAt >= :start AND l.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
