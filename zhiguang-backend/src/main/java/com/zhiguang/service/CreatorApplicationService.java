package com.zhiguang.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.dto.CreatorApplicationDto;
import com.zhiguang.dto.CreatorApplicationResponseDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.dto.ReviewDto;
import com.zhiguang.entity.ApplicationStatus;
import com.zhiguang.entity.CreatorApplication;
import com.zhiguang.entity.User;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.CreatorApplicationRepository;
import com.zhiguang.repository.UserRepository;
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
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorApplicationService {

    private static final List<String> VALID_DIRECTIONS = List.of(
            "Java 工程师", "产品经理", "测试工程师", "AI 开发者", "前端工程师", "后端工程师"
    );

    private final CreatorApplicationRepository creatorApplicationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;

    @Transactional
    public CreatorApplicationResponseDto apply(Long userId, CreatorApplicationDto dto) {
        validateDirection(dto.getDirection());
        validateWorks(dto.getWorks());

        boolean exists = creatorApplicationRepository.existsByUserIdAndDirectionAndStatusIn(
                userId,
                dto.getDirection(),
                List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED)
        );
        if (exists) {
            throw new BadRequestException("您已提交过该方向的认证申请，请勿重复提交");
        }

        CreatorApplication application = new CreatorApplication();
        application.setUserId(userId);
        application.setDirection(dto.getDirection());
        application.setCredentials(toCredentialsJson(dto));
        application.setStatus(ApplicationStatus.PENDING);

        CreatorApplication saved = creatorApplicationRepository.save(application);
        return toResponseDto(saved);
    }

    public CreatorApplicationResponseDto getMyApplication(Long userId) {
        CreatorApplication application = creatorApplicationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        return application != null ? toResponseDto(application) : null;
    }

    @Transactional
    public CreatorApplicationResponseDto review(Long applicationId, ReviewDto dto, Long reviewerUserId) {
        if (!isAdmin(reviewerUserId)) {
            throw new BadRequestException("无权操作");
        }

        CreatorApplication application = creatorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("认证申请", "id", applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("该申请已被处理，无法重复审核");
        }

        if (dto.getStatus() != ApplicationStatus.APPROVED && dto.getStatus() != ApplicationStatus.REJECTED) {
            throw new BadRequestException("审核状态非法");
        }

        application.setStatus(dto.getStatus());
        application.setReviewReason(dto.getReviewReason());
        application.setReviewedAt(LocalDateTime.now());

        if (dto.getStatus() == ApplicationStatus.APPROVED) {
            User user = userRepository.findById(application.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("用户", "id", application.getUserId()));
            if (!user.getCreatorBadges().contains(application.getDirection())) {
                user.getCreatorBadges().add(application.getDirection());
            }
            // 将用户角色升级为创作者
            if (!"CREATOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
                user.setRole("CREATOR");
            }
            userRepository.save(user);
            evictUserCache(user.getId());
        }

        CreatorApplication saved = creatorApplicationRepository.save(application);
        return toResponseDto(saved);
    }

    public PageResponse<CreatorApplicationResponseDto> listApplications(ApplicationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CreatorApplication> applicationPage = status != null
                ? creatorApplicationRepository.findByStatus(status, pageable)
                : creatorApplicationRepository.findAll(pageable);

        List<CreatorApplicationResponseDto> list = applicationPage.getContent().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                list,
                applicationPage.getTotalElements(),
                applicationPage.getNumber(),
                applicationPage.getSize(),
                applicationPage.getTotalPages()
        );
    }

    private void validateDirection(String direction) {
        if (!VALID_DIRECTIONS.contains(direction)) {
            throw new BadRequestException("无效的认证方向");
        }
    }

    private void validateWorks(List<String> works) {
        if (works == null || works.isEmpty()) {
            throw new BadRequestException("请至少填写一个作品链接");
        }
        for (String work : works) {
            if (work == null || work.isBlank()) {
                throw new BadRequestException("作品链接不能为空");
            }
            if (work.length() > 500) {
                throw new BadRequestException("作品链接长度不能超过500");
            }
        }
    }

    private String toCredentialsJson(CreatorApplicationDto dto) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("bio", dto.getBio());
        credentials.put("works", dto.getWorks());
        try {
            return objectMapper.writeValueAsString(credentials);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("认证资料格式错误");
        }
    }

    private CreatorApplicationResponseDto toResponseDto(CreatorApplication application) {
        Map<String, Object> credentials = parseCredentials(application.getCredentials());
        String bio = (String) credentials.getOrDefault("bio", "");
        @SuppressWarnings("unchecked")
        List<String> works = (List<String>) credentials.getOrDefault("works", List.of());

        return new CreatorApplicationResponseDto(
                application.getId(),
                application.getUserId(),
                application.getDirection(),
                bio,
                works,
                application.getStatus(),
                application.getReviewReason(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private Map<String, Object> parseCredentials(String credentials) {
        if (credentials == null || credentials.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(credentials, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析认证资料失败: {}", credentials, e);
            return Map.of();
        }
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
    }

    private void evictUserCache(Long userId) {
        cacheService.evict("user:dto:" + userId);
        cacheService.evict("user:stats:" + userId);
    }
}
