package com.zhiguang.repository;

import com.zhiguang.entity.Article;
import com.zhiguang.entity.ArticleFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleFavoriteRepository extends JpaRepository<ArticleFavorite, Long> {

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    Optional<ArticleFavorite> findByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    long countByArticleId(Long articleId);

    long countByUser_Id(Long userId);

    @Query("SELECT f.article.id FROM ArticleFavorite f WHERE f.user.id = :userId AND f.article.id IN :articleIds")
    List<Long> findFavoritedArticleIds(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);

    @Query("SELECT f.article FROM ArticleFavorite f WHERE f.user.id = :userId AND f.article.status = 2 ORDER BY f.createdAt DESC")
    Page<Article> findPublishedFavorites(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM ArticleFavorite f JOIN FETCH f.article a JOIN FETCH a.author WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<ArticleFavorite> findByUserIdWithArticle(@Param("userId") Long userId);
}
