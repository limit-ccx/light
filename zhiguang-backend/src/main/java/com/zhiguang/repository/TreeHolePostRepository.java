package com.zhiguang.repository;

import com.zhiguang.entity.TreeHolePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreeHolePostRepository extends JpaRepository<TreeHolePost, Long> {

    Page<TreeHolePost> findByStatusOrderByScoreDescCreatedAtDesc(Integer status, Pageable pageable);

    Page<TreeHolePost> findByStatusAndEmotionTagOrderByScoreDescCreatedAtDesc(Integer status, String emotionTag, Pageable pageable);

    Page<TreeHolePost> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);

    Page<TreeHolePost> findByStatusAndEmotionTagOrderByCreatedAtDesc(Integer status, String emotionTag, Pageable pageable);

    long countByAnonymousHashAndCreatedAtAfter(String anonymousHash, LocalDateTime after);

    long countByStatusAndCreatedAtAfter(Integer status, LocalDateTime after);

    long countByStatus(Integer status);

    @Query("SELECT p.emotionTag, COUNT(p) FROM TreeHolePost p WHERE p.status = :status GROUP BY p.emotionTag")
    List<Object[]> countByStatusGroupByEmotionTag(@Param("status") Integer status);

    @Modifying
    @Query("UPDATE TreeHolePost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TreeHolePost p SET p.replyCount = p.replyCount + 1 WHERE p.id = :id")
    void incrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TreeHolePost p SET p.replyCount = GREATEST(0, p.replyCount - 1) WHERE p.id = :id")
    void decrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TreeHolePost p SET p.reportCount = p.reportCount + 1 WHERE p.id = :id")
    void incrementReportCount(@Param("id") Long id);

    @Query("SELECT p.emotionTag, COUNT(p), FUNCTION('DATE', p.createdAt) FROM TreeHolePost p " +
           "WHERE p.createdAt >= :since AND p.status = 1 " +
           "GROUP BY p.emotionTag, FUNCTION('DATE', p.createdAt) " +
           "ORDER BY FUNCTION('DATE', p.createdAt)")
    List<Object[]> getEmotionTrends(@Param("since") LocalDateTime since);
}
