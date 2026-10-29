package com.zhiguang.repository;

import com.zhiguang.entity.TreeHoleReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreeHoleReactionRepository extends JpaRepository<TreeHoleReaction, Long> {

    Optional<TreeHoleReaction> findByAnonymousHashAndTargetTypeAndTargetIdAndReactionType(
            String anonymousHash, String targetType, Long targetId, String reactionType);

    boolean existsByAnonymousHashAndTargetTypeAndTargetIdAndReactionType(
            String anonymousHash, String targetType, Long targetId, String reactionType);

    long countByTargetTypeAndTargetIdAndReactionType(String targetType, Long targetId, String reactionType);

    @Modifying
    @Query("DELETE FROM TreeHoleReaction r WHERE r.targetType = :targetType AND r.targetId = :targetId AND r.reactionType = :reactionType AND r.anonymousHash = :anonymousHash")
    void deleteReaction(@Param("anonymousHash") String anonymousHash,
                        @Param("targetType") String targetType,
                        @Param("targetId") Long targetId,
                        @Param("reactionType") String reactionType);

    long countByAnonymousHashAndCreatedAtAfter(String anonymousHash, LocalDateTime after);

    List<TreeHoleReaction> findByAnonymousHashAndTargetTypeAndTargetIdIn(
            String anonymousHash, String targetType, List<Long> targetIds);

    List<TreeHoleReaction> findByAnonymousHash(String anonymousHash);
}
