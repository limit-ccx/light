package com.zhiguang.service;

import com.zhiguang.dto.FeedbackResponseDto;
import com.zhiguang.dto.FeedbackSubmitDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.User;
import com.zhiguang.entity.UserFeedback;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.UserFeedbackRepository;
import com.zhiguang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final UserFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submit(Long userId, FeedbackSubmitDto dto) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setType(dto.getType());
        feedback.setTitle(dto.getTitle());
        feedback.setContent(dto.getContent());
        feedback.setContact(dto.getContact());
        feedback.setStatus("PENDING");
        feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponseDto> page(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserFeedback> result = (status != null && !status.isBlank())
                ? feedbackRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<UserFeedback> rows = result.getContent();
        Map<Long, String> names = loadUsernames(rows);
        List<FeedbackResponseDto> list = rows.stream()
                .map(row -> toDto(row, names.get(row.getUserId())))
                .toList();

        return new PageResponse<>(list, result.getTotalElements(), result.getNumber(),
                result.getSize(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long pending = feedbackRepository.countByStatus("PENDING");
        long total = feedbackRepository.count();
        return Map.of(
                "pending", pending,
                "handled", total - pending,
                "total", total
        );
    }

    @Transactional
    public void handle(Long id, String status, String reply) {
        UserFeedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("反馈", "id", id));
        feedback.setStatus("HANDLED".equals(status) ? "HANDLED" : "PENDING");
        feedback.setReply(reply);
        feedbackRepository.save(feedback);
    }

    private Map<Long, String> loadUsernames(List<UserFeedback> rows) {
        List<Long> ids = rows.stream()
                .map(UserFeedback::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private FeedbackResponseDto toDto(UserFeedback row, String username) {
        return new FeedbackResponseDto(
                row.getId(),
                row.getUserId(),
                username,
                row.getType(),
                row.getTitle(),
                row.getContent(),
                row.getContact(),
                row.getStatus(),
                row.getReply(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}
