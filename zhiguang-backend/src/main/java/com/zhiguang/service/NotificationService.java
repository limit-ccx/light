package com.zhiguang.service;

import com.zhiguang.entity.Notification;
import com.zhiguang.entity.User;
import com.zhiguang.repository.NotificationRepository;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public void sendNotification(Long userId, String type, String content, Long articleId) {
        User user = userService.getUserById(userId);
        Notification notification = new Notification(user, type, content, articleId);
        notificationRepository.save(notification);

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "NEW_NOTIFICATION");
        payload.put("notificationId", notification.getId());
        payload.put("notificationType", type);
        payload.put("content", content);
        payload.put("articleId", articleId);
        payload.put("createdAt", notification.getCreatedAt());
        sseEmitterService.sendToUser(userId, com.zhiguang.service.SseEmitterService.CHANNEL_NOTIFICATION, payload);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository.markAsRead(id, userId);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        notificationRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(notificationRepository::delete);
    }
}
