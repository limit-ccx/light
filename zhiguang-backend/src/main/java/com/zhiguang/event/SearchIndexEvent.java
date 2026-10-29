package com.zhiguang.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索索引同步事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchIndexEvent {

    private Long articleId;       // 文章ID
    private String action;        // 操作类型：CREATE, UPDATE, DELETE
    private LocalDateTime timestamp;
}
