package com.zhiguang.consumer;

import com.zhiguang.event.NotificationEvent;
import com.zhiguang.event.ArticleViewSyncEvent;
import com.zhiguang.event.SearchIndexEvent;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.service.CacheService;
import com.zhiguang.service.ElasticsearchService;
import com.zhiguang.service.NotificationService;
import com.zhiguang.service.UserGrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息消费者
 * <p>
 * 处理各类异步消息：
 * - 通知消息：保存通知并推送 SSE
 * - 浏览量同步：将 Redis 增量原子回写数据库
 * - 搜索索引同步：将文章索引到 Elasticsearch
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final NotificationService notificationService;
    private final ArticleRepository articleRepository;
    private final ElasticsearchService elasticsearchService;
    private final CacheService cacheService;
    private final UserGrowthService userGrowthService;

    // 注意：这里不吞异常——抛出后由容器按 spring.rabbitmq.listener.simple.retry
    // 配置重试，重试耗尽进 DLQ 留痕（此前 try-catch 吞掉导致重试配置永不生效、失败即丢失）
    @RabbitListener(queues = "notification.queue")
    public void handleNotification(NotificationEvent event) {
        log.debug("收到通知消息: userId={}, type={}", event.getUserId(), event.getType());
        notificationService.sendNotification(event.getUserId(), event.getType(), event.getContent(), event.getRelatedId());
    }

    @RabbitListener(queues = "article.view.sync.queue")
    @Transactional
    public void handleArticleViewSync(ArticleViewSyncEvent event) {
        log.debug("收到浏览量同步消息: articleId={}, increment={}", event.getArticleId(), event.getIncrement());
        if (event.getIncrement() <= 0) {
            return;
        }

        Integer oldViewCount;
        Long authorId;
        int increment;
        try {
            oldViewCount = articleRepository.findViewCountById(event.getArticleId());
            authorId = articleRepository.findAuthorIdById(event.getArticleId());
            if (oldViewCount == null) {
                return;
            }

            increment = event.getIncrement() > Integer.MAX_VALUE ? Integer.MAX_VALUE : event.getIncrement().intValue();
            articleRepository.increaseViewCount(event.getArticleId(), increment);
            cacheService.evict("article:" + event.getArticleId());
        } catch (Exception e) {
            // 仅 DB 更新失败才把增量还回 Redis（由定时任务下轮重新同步）；
            // 消息本身确认消费，避免重投造成 DB 重复累加
            cacheService.restoreViewCounter(event.getArticleId(), event.getIncrement());
            log.error("浏览量同步失败，增量已还回 Redis: articleId={}, error={}", event.getArticleId(), e.getMessage(), e);
            return;
        }

        log.info("浏览量同步成功: articleId={}, increment={}", event.getArticleId(), increment);

        // 成长体系检查失败不影响计数主流程（此前它抛异常会误触发上面的"还回增量"，造成重复计数）
        if (authorId != null && oldViewCount < 1000 && oldViewCount + increment >= 1000) {
            try {
                userGrowthService.checkAchievements(authorId);
            } catch (Exception e) {
                log.error("浏览量成就检查失败: authorId={}", authorId, e);
            }
        }
    }

    @RabbitListener(queues = "search.index.queue")
    public void handleSearchIndexSync(SearchIndexEvent event) {
        log.debug("收到搜索索引消息: articleId={}, action={}", event.getArticleId(), event.getAction());
        switch (event.getAction()) {
            case "CREATE":
            case "UPDATE":
                articleRepository.findById(event.getArticleId()).ifPresent(article -> {
                    elasticsearchService.indexArticle(article);
                    log.info("搜索索引同步成功: articleId={}, action={}", event.getArticleId(), event.getAction());
                });
                break;
            case "DELETE":
                elasticsearchService.deleteArticleIndex(event.getArticleId());
                log.info("搜索索引删除成功: articleId={}", event.getArticleId());
                break;
            default:
                log.warn("未知的索引操作: action={}", event.getAction());
        }
    }
}
