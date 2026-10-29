package com.zhiguang.repository;

import com.zhiguang.entity.UserAvatarUnlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAvatarUnlockRepository extends JpaRepository<UserAvatarUnlock, Long> {

    List<UserAvatarUnlock> findByUserId(Long userId);

    boolean existsByUserIdAndAvatarId(Long userId, String avatarId);
}
