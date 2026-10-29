package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.ConversationSummaryDto;
import com.zhiguang.dto.MessageResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.Message;
import com.zhiguang.entity.User;
import com.zhiguang.service.MessageService;
import com.zhiguang.service.SseEmitterService;
import com.zhiguang.service.UserService;
import com.zhiguang.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final SseEmitterService sseEmitterService;

    @PostMapping("/{receiverId}")
    public ResponseEntity<ApiResponse<MessageResponseDto>> sendMessage(
            @PathVariable Long receiverId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Message message = messageService.sendMessage(user.getId(), receiverId, body.get("content"), body.get("clientMessageId"));
        return ResponseEntity.ok(ApiResponse.success(MessageResponseDto.fromEntity(message)));
    }

    @GetMapping("/stream")
    public SseEmitter streamMessages(@AuthenticationPrincipal User user) {
        if (user == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("未登录或登录已过期"));
            return emitter;
        }
        return sseEmitterService.subscribe(user.getId(), com.zhiguang.service.SseEmitterService.CHANNEL_MESSAGE);
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponseDto>>> getConversation(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        Page<Message> conversation = messageService.getConversation(user.getId(), userId, page, size);
        List<MessageResponseDto> content = conversation.getContent().stream()
                .map(MessageResponseDto::fromEntity)
                .collect(Collectors.toList());
        PageResponse<MessageResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(conversation.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(conversation.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/conversation/{userId}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        messageService.markConversationAsRead(user.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = user == null ? 0L : messageService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> getConversationList(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success(messageService.getConversationList(user.getId())));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<MessageResponseDto.UserBriefDto>>> searchUsers(
            @RequestParam String keyword,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        List<User> users = userService.searchUsers(keyword, user.getId());
        List<MessageResponseDto.UserBriefDto> dtoList = users.stream()
                .map(u -> new MessageResponseDto.UserBriefDto(u.getId(), u.getUsername(), u.getAvatar()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    /** 好友列表：互相关注的用户（发起新对话默认展示） */
    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<MessageResponseDto.UserBriefDto>>> getFriends(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success(messageService.getFriendList(user.getId())));
    }

    /**
     * 与某用户的聊天资格：未互关只能发一条打招呼消息，
     * 前端据此禁用输入区并提示"互相关注后可继续聊天"。
     */
    @GetMapping("/chat-status/{partnerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatStatus(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success(messageService.getChatStatus(user.getId(), partnerId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponseDto>>> searchMessages(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        Page<Message> result = messageService.searchMessages(user.getId(), keyword, page, size);
        List<MessageResponseDto> content = result.getContent().stream()
                .map(MessageResponseDto::fromEntity)
                .collect(Collectors.toList());
        PageResponse<MessageResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search/conversation/{partnerId}")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponseDto>>> searchConversationMessages(
            @PathVariable Long partnerId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        Page<Message> result = messageService.searchConversationMessages(user.getId(), partnerId, keyword, page, size);
        List<MessageResponseDto> content = result.getContent().stream()
                .map(MessageResponseDto::fromEntity)
                .collect(Collectors.toList());
        PageResponse<MessageResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search/sender/{senderId}")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponseDto>>> searchMessagesBySender(
            @PathVariable Long senderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        page = PageUtil.clampPage(page);
        size = PageUtil.clampSize(size);
        Page<Message> result = messageService.searchMessagesBySender(user.getId(), senderId, page, size);
        List<MessageResponseDto> content = result.getContent().stream()
                .map(MessageResponseDto::fromEntity)
                .collect(Collectors.toList());
        PageResponse<MessageResponseDto> response = new PageResponse<>();
        response.setList(content);
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversation/{partnerId}/before")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> findMessagesBefore(
            @PathVariable Long partnerId,
            @RequestParam LocalDateTime before,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        List<Message> list = messageService.findMessagesBefore(user.getId(), partnerId, before, limit).getContent();
        List<MessageResponseDto> dtoList = list.stream()
                .map(MessageResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        messageService.deleteMessage(messageId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
