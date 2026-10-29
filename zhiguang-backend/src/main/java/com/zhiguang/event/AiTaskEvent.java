package com.zhiguang.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 任务事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskEvent {

    private Long taskId;          // 任务ID
    private String type;          // 任务类型：POLISH, SUMMARIZE, TITLE_GENERATION, RAG
    private Long userId;          // 用户ID
    private Long articleId;       // 文章ID（可选）
    private String content;       // 输入内容
    private LocalDateTime timestamp;
}
