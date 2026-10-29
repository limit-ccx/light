package com.zhiguang.service;

import com.zhiguang.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息队列服务
 * <p>
 * 提供消息发送功能，用于异步处理：
 * - 通知异步发送
 * - 浏览量同步回写
 * - AI 任务异步处理
 * - 搜索索引同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送通知消息
     */
    public void sendNotification(Long userId, String type, String content, Long relatedId) {
        NotificationEvent event = new NotificationEvent(userId, type, content, relatedId, LocalDateTime.now());
        rabbitTemplate.convertAndSend("notification.exchange", "notification.routing", event);
        log.debug("发送通知消息: userId={}, type={}, content={}", userId, type, content);
    }

    /**
     * 发送浏览量同步消息
     */
    public void syncArticleView(Long articleId, Long increment) {
        ArticleViewSyncEvent event = new ArticleViewSyncEvent(articleId, increment, LocalDateTime.now());
        rabbitTemplate.convertAndSend("article.exchange", "article.view.sync.routing", event);
        log.debug("发送浏览量同步消息: articleId={}, increment={}", articleId, increment);
    }

    /**
     * 发送 AI 任务消息
     */
    public void sendAiTask(Long taskId, String type, Long userId, Long articleId, String content) {
        AiTaskEvent event = new AiTaskEvent(taskId, type, userId, articleId, content, LocalDateTime.now());
        rabbitTemplate.convertAndSend("ai.exchange", "ai.task.routing", event);
        log.debug("发送 AI 任务消息: taskId={}, type={}, userId={}", taskId, type, userId);
    }

    /**
     * 发送搜索索引同步消息
     */
    public void syncSearchIndex(Long articleId, String action) {
        SearchIndexEvent event = new SearchIndexEvent(articleId, action, LocalDateTime.now());
        rabbitTemplate.convertAndSend("search.exchange", "search.index.routing", event);
        log.debug("发送搜索索引同步消息: articleId={}, action={}", articleId, action);
    }

    /**
     * 发送文章创建索引消息
     */
    public void createSearchIndex(Long articleId) {
        syncSearchIndex(articleId, "CREATE");
    }

    /**
     * 发送文章更新索引消息
     */
    public void updateSearchIndex(Long articleId) {
        syncSearchIndex(articleId, "UPDATE");
    }

    /**
     * 发送文章删除索引消息
     */
    public void deleteSearchIndex(Long articleId) {
        syncSearchIndex(articleId, "DELETE");
    }
}