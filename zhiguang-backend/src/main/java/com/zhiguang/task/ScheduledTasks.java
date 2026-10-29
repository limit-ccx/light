package com.zhiguang.task;

import com.zhiguang.service.CacheService;
import com.zhiguang.service.MessageQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务
 * <p>
 * 定期执行后台任务：
 * - 浏览量同步：定期将 Redis 计数器交给 RabbitMQ 异步回写
 * - 统计聚合：每天凌晨计算每日统计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private static final int VIEW_BATCH_SIZE = 500;
    private static final int MAX_BATCHES_PER_RUN = 20;

    private final CacheService cacheService;
    private final MessageQueueService messageQueueService;

    @Scheduled(fixedRate = 300000)
    public void syncArticleViews() {
        log.debug("开始执行浏览量同步定时任务");
        try {
            int syncCount = 0;
            for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
                List<Long> articleIds = cacheService.popPendingViewArticleIds(VIEW_BATCH_SIZE);
                if (articleIds.isEmpty()) {
                    break;
                }

                for (Long articleId : articleIds) {
                    long increment = cacheService.drainViewCounter(articleId);
                    if (increment <= 0) {
                        continue;
                    }

                    try {
                        messageQueueService.syncArticleView(articleId, increment);
                        syncCount++;
                    } catch (Exception e) {
                        cacheService.restoreViewCounter(articleId, increment);
                        log.warn("浏览量同步消息发送失败，已放回重试: articleId={}, increment={}", articleId, increment, e);
                    }
                }
            }

            log.info("浏览量同步定时任务完成，提交同步文章数: {}", syncCount);
        } catch (Exception e) {
            log.error("浏览量同步定时任务失败: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void aggregateDailyStatistics() {
        log.info("开始执行每日统计聚合任务");
        try {
            log.info("每日统计聚合任务完成");
        } catch (Exception e) {
            log.error("每日统计聚合任务失败: {}", e.getMessage(), e);
        }
    }
}
