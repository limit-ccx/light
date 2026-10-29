package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项：伙伴摘要 + 最后一条消息预览 + 真实未读数（M-4）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDto {

    private MessageResponseDto.UserBriefDto partner;

    /** 最后一条消息预览（内容截断到 100 字） */
    private String lastMessagePreview;

    private LocalDateTime lastMessageAt;

    /** 该会话未读数（真实统计值） */
    private long unreadCount;

    /** 最后一条消息是否为自己发出（前端决定预览前缀） */
    private boolean lastMessageFromMe;

    /** 是否已互相关注（好友）；false 时会话标记"待互关"且受单条私信限制 */
    private boolean mutualFollow;
}
