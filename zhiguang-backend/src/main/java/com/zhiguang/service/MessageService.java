package com.zhiguang.service;

import com.zhiguang.dto.ConversationSummaryDto;
import com.zhiguang.dto.MessageResponseDto;
import com.zhiguang.entity.Message;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /** 单条私信长度上限（M-2：JSON body 无服务端上限，防止巨型消息打内存/DB） */
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int PREVIEW_LENGTH = 100;

    /** 未互关（非好友）时每个方向最多发送的私信条数（打招呼） */
    private static final int STRANGER_MESSAGE_LIMIT = 1;
    private static final String NOT_MUTUAL_MSG = "你们还未互相关注，只能先发送一条消息；互相关注成为好友后即可继续畅聊";

    /** 是否互相关注（好友） */
    public boolean isMutualFollow(Long userId, Long partnerId) {
        if (userId == null || partnerId == null || userId.equals(partnerId)) {
            return false;
        }
        return userFollowRepository.existsByFollowerIdAndFollowingId(userId, partnerId)
                && userFollowRepository.existsByFollowerIdAndFollowingId(partnerId, userId);
    }

    /** 好友集合：我关注的 ∩ 关注我的（两条查询，避免逐会话点查） */
    private java.util.Set<Long> mutualFriendIds(Long userId) {
        java.util.Set<Long> iFollow = new java.util.HashSet<>();
        for (com.zhiguang.entity.UserFollow f : userFollowRepository.findByFollowerId(userId)) {
            if (f.getFollowing() != null && f.getFollowing().getId() != null) {
                iFollow.add(f.getFollowing().getId());
            }
        }
        java.util.Set<Long> followersOfMe = new java.util.HashSet<>();
        for (com.zhiguang.entity.UserFollow f : userFollowRepository.findByFollowingId(userId)) {
            if (f.getFollower() != null && f.getFollower().getId() != null) {
                followersOfMe.add(f.getFollower().getId());
            }
        }
        iFollow.retainAll(followersOfMe);
        iFollow.remove(userId);
        return iFollow;
    }

    /** 未互关时该方向是否还能发送（打招呼额度未用完） */
    public boolean canSendTo(Long senderId, Long receiverId) {
        if (isMutualFollow(senderId, receiverId)) {
            return true;
        }
        return messageRepository.countBySenderIdAndReceiverId(senderId, receiverId) < STRANGER_MESSAGE_LIMIT;
    }

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;
    private final ContentModerationService contentModerationService;
    private final com.zhiguang.repository.UserFollowRepository userFollowRepository;
    private final com.zhiguang.repository.UserRepository userRepository;

    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, String content, String clientMessageId) {
        if (senderId.equals(receiverId)) {
            throw new BadRequestException("不能给自己发私信");
        }
        // 未互关（非好友）只能发一条打招呼消息；计数忽略删除标记，防止"删了再发"绕过
        if (!canSendTo(senderId, receiverId)) {
            throw new BadRequestException(NOT_MUTUAL_MSG);
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BadRequestException("消息内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("消息内容不能超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        // 私信同样过敏感词校验（M-2：此前完全绕过审核）
        contentModerationService.validate(content);

        // 幂等：clientMessageId 限定发送者属主查询（M-1：全局查找会把他人消息返回给调用者）
        if (clientMessageId != null && !clientMessageId.isBlank()) {
            Message existing = messageRepository
                    .findByClientMessageIdAndSenderId(clientMessageId, senderId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(receiverId);
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content.trim());
        message.setClientMessageId(clientMessageId);
        message.setStatus(com.zhiguang.entity.MessageStatus.SENT);
        message = messageRepository.save(message);

        String notifContent = sender.getUsername() + " 给你发了一条私信";
        notificationService.sendNotification(receiverId, "MESSAGE", notifContent, null);

        MessageResponseDto dto = MessageResponseDto.fromEntity(message);

        // NEW_MESSAGE 附带双方用户摘要（M-5：陌生人首条私信前端无需回源即可渲染会话）
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_MESSAGE");
        payload.put("messageId", message.getId());
        payload.put("senderId", senderId);
        payload.put("receiverId", receiverId);
        payload.put("sender", dto.getSender());
        payload.put("receiver", dto.getReceiver());
        payload.put("content", message.getContent());
        payload.put("clientMessageId", clientMessageId);
        payload.put("isRead", false);
        payload.put("createdAt", message.getCreatedAt());
        sseEmitterService.sendToUser(receiverId, SseEmitterService.CHANNEL_MESSAGE, payload);

        Map<String, Object> ackPayload = new HashMap<>();
        ackPayload.put("type", "MESSAGE_SENT");
        ackPayload.put("messageId", message.getId());
        ackPayload.put("clientMessageId", clientMessageId);
        ackPayload.put("status", message.getStatus().name());
        ackPayload.put("message", dto);
        sseEmitterService.sendToUser(senderId, SseEmitterService.CHANNEL_MESSAGE, ackPayload);

        return message;
    }

    @Transactional(readOnly = true)
    public Page<Message> getConversation(Long userId, Long otherId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findConversation(userId, otherId, userId, pageable);
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long otherId) {
        messageRepository.markConversationAsRead(userId, otherId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return messageRepository.countUnreadByReceiverId(userId);
    }

    /**
     * 会话列表：最新一条"对我可见"消息 + 按发送者分组的真实未读数（M-4/M-7）。
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversationList(Long userId) {
        Map<Long, Long> unreadBySender = new HashMap<>();
        for (Object[] row : messageRepository.countUnreadGroupBySender(userId)) {
            unreadBySender.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        java.util.Set<Long> mutual = mutualFriendIds(userId);
        List<Message> latestList = messageRepository.findLatestMessagesByUserId(userId);
        return latestList.stream()
                .map(m -> {
                    boolean fromMe = m.getSender().getId().equals(userId);
                    User partner = fromMe ? m.getReceiver() : m.getSender();
                    String preview = m.getContent() == null ? "" : m.getContent();
                    if (preview.length() > PREVIEW_LENGTH) {
                        preview = preview.substring(0, PREVIEW_LENGTH) + "…";
                    }
                    return new ConversationSummaryDto(
                            new MessageResponseDto.UserBriefDto(
                                    partner.getId(), partner.getUsername(), partner.getAvatar()),
                            preview,
                            m.getCreatedAt(),
                            unreadBySender.getOrDefault(partner.getId(), 0L),
                            fromMe,
                            mutual.contains(partner.getId()));
                })
                .toList();
    }

    /**
     * 好友列表：互相关注的用户（发起新对话对话框默认展示）。
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDto.UserBriefDto> getFriendList(Long userId) {
        java.util.Set<Long> mutual = mutualFriendIds(userId);
        if (mutual.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(mutual).stream()
                .map(u -> new MessageResponseDto.UserBriefDto(u.getId(), u.getUsername(), u.getAvatar()))
                .sorted(java.util.Comparator.comparing(MessageResponseDto.UserBriefDto::getUsername))
                .toList();
    }

    /**
     * 与某用户的聊天资格状态（前端据此禁用输入并提示）。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChatStatus(Long userId, Long partnerId) {
        boolean mutual = isMutualFollow(userId, partnerId);
        boolean canSend = mutual
                || messageRepository.countBySenderIdAndReceiverId(userId, partnerId) < STRANGER_MESSAGE_LIMIT;
        Map<String, Object> status = new HashMap<>();
        status.put("mutualFollow", mutual);
        status.put("canSend", canSend);
        status.put("strangerLimit", STRANGER_MESSAGE_LIMIT);
        return status;
    }

    /**
     * 删除：仅对自己删除（M-3）。双方均删除时物理清理。
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("消息不存在"));
        boolean isSender = message.getSender().getId().equals(userId);
        boolean isReceiver = message.getReceiver().getId().equals(userId);
        if (!isSender && !isReceiver) {
            throw new BadRequestException("无权删除此消息");
        }
        if (isSender) {
            message.setDeletedForSender(true);
        }
        if (isReceiver) {
            message.setDeletedForReceiver(true);
        }
        if (message.isDeletedForSender() && message.isDeletedForReceiver()) {
            messageRepository.delete(message);
        } else {
            messageRepository.save(message);
        }
    }

    @Transactional(readOnly = true)
    public Page<Message> searchMessages(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.searchMessages(userId, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> searchConversationMessages(Long userId, Long partnerId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.searchConversationMessages(userId, partnerId, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> searchMessagesBySender(Long userId, Long senderId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.searchMessagesBySender(userId, senderId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> findMessagesBefore(Long userId, Long partnerId, LocalDateTime before, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return messageRepository.findMessagesBefore(userId, partnerId, before, pageable);
    }
}
