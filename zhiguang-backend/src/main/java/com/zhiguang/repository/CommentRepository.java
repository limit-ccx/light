package com.zhiguang.repository;

import com.zhiguang.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "article"})
    Page<Comment> findByArticleIdAndParentIsNull(Long articleId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "article", "parent.user"})
    @Query("SELECT c FROM Comment c WHERE c.parent.id IN :parentIds ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.article.id = :articleId")
    Long countByArticleId(@Param("articleId") Long articleId);

    @EntityGraph(attributePaths = {"user", "article"})
    @Query("SELECT c FROM Comment c WHERE c.article.author.id = :userId ORDER BY c.createdAt DESC")
    List<Comment> findRecentCommentsByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);

    // ========== 运营后台 ==========

    @Query("SELECT c FROM Comment c JOIN FETCH c.article JOIN FETCH c.user WHERE " +
            "(:status IS NULL OR c.status = :status) " +
            "AND (:keyword IS NULL OR c.content LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> searchForAdmin(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE DATE(c.createdAt) = CURRENT_DATE")
    long countByCreatedToday();

    long countByStatus(String status);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt >= :start AND c.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
