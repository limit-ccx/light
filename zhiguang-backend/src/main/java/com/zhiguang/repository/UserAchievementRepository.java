package com.zhiguang.repository;

import com.zhiguang.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserIdOrderByObtainedAtDesc(Long userId);

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    @Query("SELECT ua.achievement.id FROM UserAchievement ua WHERE ua.userId = :userId")
    Set<Long> findAchievementIdsByUserId(@Param("userId") Long userId);
}
