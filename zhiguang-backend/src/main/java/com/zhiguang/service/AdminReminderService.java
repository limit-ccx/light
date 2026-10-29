package com.zhiguang.service;

import com.zhiguang.dto.PageResponse;
import com.zhiguang.dto.ReminderCreateDto;
import com.zhiguang.dto.ReminderResponseDto;
import com.zhiguang.entity.AdminReminder;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.AdminReminderRepository;
import com.zhiguang.repository.UserRepository;
import com.zhiguang.security.AdminPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 催办单：超级管理员发起 → 运营管理接收并回执完成。 */
@Service
@RequiredArgsConstructor
public class AdminReminderService {

    private final AdminReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final AdminLogService adminLogService;

    @Transactional
    public void create(User sender, ReminderCreateDto dto) {
        if (!AdminPermissions.isSuper(sender.getAdminPosition())) {
            throw new BadRequestException("仅超级管理员可发起催办");
        }
        User target = userRepository.findById(dto.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("用户", "id", dto.getToUserId()));
        if (!"ADMIN".equals(target.getRole())
                || !AdminPermissions.isAdminManagerOnly(target.getAdminPosition())) {
            throw new BadRequestException("催办对象必须是运营管理");
        }
        if (target.getId().equals(sender.getId())) {
            throw new BadRequestException("不能给自己发催办");
        }

        AdminReminder reminder = new AdminReminder();
        reminder.setFromUserId(sender.getId());
        reminder.setToUserId(target.getId());
        reminder.setTitle(dto.getTitle().trim());
        reminder.setContent(dto.getContent() == null ? null : dto.getContent().trim());
        reminder.setStatus("PENDING");
        reminderRepository.save(reminder);

        adminLogService.log(sender.getId(), "SEND_REMINDER", "ADMIN_REMINDER", reminder.getId(),
                "催办 " + target.getUsername() + "：" + reminder.getTitle());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderResponseDto> received(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminReminder> result = (status != null && !status.isBlank())
                ? reminderRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : reminderRepository.findByToUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderResponseDto> sent(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminReminder> result = (status != null && !status.isBlank())
                ? reminderRepository.findByFromUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : reminderRepository.findByFromUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPage(result);
    }

    @Transactional
    public void complete(Long id, Long userId, String reply) {
        AdminReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("催办", "id", id));
        if (!reminder.getToUserId().equals(userId)) {
            throw new BadRequestException("只能处理发给自己的催办");
        }
        if ("DONE".equals(reminder.getStatus())) {
            throw new BadRequestException("该催办已完成");
        }
        reminder.setStatus("DONE");
        reminder.setReply(reply == null ? null : reply.trim());
        reminder.setCompletedAt(LocalDateTime.now());
        reminderRepository.save(reminder);

        adminLogService.log(userId, "COMPLETE_REMINDER", "ADMIN_REMINDER", id,
                "完成催办：" + reminder.getTitle());
    }

    @Transactional
    public void acknowledge(Long id, Long userId) {
        AdminReminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("催办", "id", id));
        if (!reminder.getFromUserId().equals(userId)) {
            throw new BadRequestException("只能确认自己发起的催办");
        }
        if (!"DONE".equals(reminder.getStatus())) {
            throw new BadRequestException("对方尚未完成，无法确认");
        }
        reminder.setAcknowledgedAt(LocalDateTime.now());
        reminderRepository.save(reminder);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<AdminReminder> rows = reminderRepository.findByToUserIdAndReadAtIsNull(userId);
        if (rows.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        rows.forEach(r -> r.setReadAt(now));
        reminderRepository.saveAll(rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("unread", reminderRepository.countByToUserIdAndReadAtIsNull(userId));
        result.put("receivedPending", reminderRepository.countByToUserIdAndStatus(userId, "PENDING"));
        result.put("sentCompleted", reminderRepository
                .countByFromUserIdAndStatusAndAcknowledgedAtIsNull(userId, "DONE"));
        return result;
    }

    private PageResponse<ReminderResponseDto> toPage(Page<AdminReminder> result) {
        Map<Long, String> names = loadUsernames(result.getContent());
        List<ReminderResponseDto> list = result.getContent().stream()
                .map(r -> new ReminderResponseDto(
                        r.getId(),
                        r.getFromUserId(),
                        names.getOrDefault(r.getFromUserId(), "未知"),
                        r.getToUserId(),
                        names.getOrDefault(r.getToUserId(), "未知"),
                        r.getTitle(),
                        r.getContent(),
                        r.getStatus(),
                        r.getReply(),
                        r.getCreatedAt(),
                        r.getReadAt(),
                        r.getCompletedAt(),
                        r.getAcknowledgedAt()))
                .toList();
        return new PageResponse<>(list, result.getTotalElements(), result.getNumber(),
                result.getSize(), result.getTotalPages());
    }

    private Map<Long, String> loadUsernames(List<AdminReminder> rows) {
        List<Long> ids = rows.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getFromUserId(), r.getToUserId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            userRepository.findAllById(ids).forEach(u -> names.put(u.getId(), u.getUsername()));
        }
        return names;
    }
}
