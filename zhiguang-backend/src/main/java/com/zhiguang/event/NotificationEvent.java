package com.zhiguang.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long userId;          // 目标用户ID
    private String type;          // 通知类型：LIKE, COMMENT, FOLLOW, SYSTEM
    private String content;       // 通知内容
    private Long relatedId;       // 关联对象ID（文章ID/评论ID等）
    private LocalDateTime timestamp;
}
