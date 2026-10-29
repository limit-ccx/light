package com.zhiguang.service;

import com.zhiguang.entity.Follow;
import com.zhiguang.entity.User;
import com.zhiguang.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CacheService cacheService;

    @Transactional
    public boolean toggleFollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        boolean exists = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        if (exists) {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
            // 失效双方用户统计缓存（粉丝/关注数）
            evictUserStatsCache(followerId);
            evictUserStatsCache(followingId);
            return false;
        } else {
            User follower = userService.getUserById(followerId);
            User following = userService.getUserById(followingId);
            Follow follow = new Follow();
            follow.setFollower(follower);
            follow.setFollowing(following);
            followRepository.save(follow);

            String content = follower.getUsername() + " 关注了你";
            notificationService.sendNotification(followingId, "FOLLOW", content, null);
            // 失效双方用户统计缓存（粉丝/关注数）
            evictUserStatsCache(followerId);
            evictUserStatsCache(followingId);
            return true;
        }
    }

    private void evictUserStatsCache(Long userId) {
        cacheService.evict("user:stats:" + userId);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    public long getFollowerCount(Long userId) {
        return followRepository.countByFollowingId(userId);
    }

    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFollowingIdsByFollowerId(userId);
    }

    public Page<Follow> getFollowingList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return followRepository.findByFollowerId(userId, pageable);
    }

    public Page<Follow> getFollowerList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return followRepository.findByFollowingId(userId, pageable);
    }
}
