package com.zhiguang.repository;

import com.zhiguang.entity.TreeHoleReply;
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
public interface TreeHoleReplyRepository extends JpaRepository<TreeHoleReply, Long> {

    Page<TreeHoleReply> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, Integer status, Pageable pageable);

    List<TreeHoleReply> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, Integer status);

    long countByPostIdAndStatus(Long postId, Integer status);

    @Modifying
    @Query("UPDATE TreeHoleReply r SET r.likeCount = r.likeCount + 1 WHERE r.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    long countByAnonymousHashAndCreatedAtAfter(String anonymousHash, LocalDateTime after);
}
