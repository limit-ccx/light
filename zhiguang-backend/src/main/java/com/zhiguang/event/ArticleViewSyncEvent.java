package com.zhiguang.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 浏览量同步事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleViewSyncEvent {

    private Long articleId;       // 文章ID
    private Long increment;       // 增量值
    private LocalDateTime timestamp;
}
