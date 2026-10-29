package com.zhiguang.dto;

import com.zhiguang.entity.Message;
import com.zhiguang.entity.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {

    private Long id;
    private UserBriefDto sender;
    private UserBriefDto receiver;
    private String content;
    private String clientMessageId;
    private MessageStatus status;
    private Boolean isRead;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBriefDto {
        private Long id;
        private String username;
        private String avatar;
    }

    public static MessageResponseDto fromEntity(Message message) {
        UserBriefDto senderDto = new UserBriefDto(
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getSender().getAvatar()
        );
        UserBriefDto receiverDto = new UserBriefDto(
                message.getReceiver().getId(),
                message.getReceiver().getUsername(),
                message.getReceiver().getAvatar()
        );
        return new MessageResponseDto(
                message.getId(),
                senderDto,
                receiverDto,
                message.getContent(),
                message.getClientMessageId(),
                message.getStatus(),
                message.getIsRead(),
                message.getCreatedAt()
        );
    }
}