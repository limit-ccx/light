package com.zhiguang.dto;

import com.zhiguang.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private String type;
    private String content;
    private Long articleId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto fromEntity(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.getArticleId(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}