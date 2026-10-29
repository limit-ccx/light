package com.zhiguang.service;

import com.zhiguang.dto.FeaturedContentDto;
import com.zhiguang.dto.PageResponse;
import com.zhiguang.entity.FeaturedContent;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.FeaturedContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRecommendService {

    private final FeaturedContentRepository featuredContentRepository;
    private final AdminLogService adminLogService;

    public PageResponse<FeaturedContent> getFeaturedContent(String position, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FeaturedContent> contentPage = (position != null && !position.isBlank())
                ? featuredContentRepository.findByPosition(position, pageable)
                : featuredContentRepository.findAll(pageable);

        return new PageResponse<>(contentPage.getContent(), contentPage.getTotalElements(),
                contentPage.getNumber(), contentPage.getSize(), contentPage.getTotalPages());
    }

    @Transactional
    public FeaturedContent addFeaturedContent(FeaturedContentDto dto, Long adminId) {
        boolean exists = featuredContentRepository.existsByTargetTypeAndTargetIdAndPosition(
                dto.getTargetType(), dto.getTargetId(), dto.getPosition());
        if (exists) {
            throw new BadRequestException("该内容已在此推荐位");
        }

        FeaturedContent content = new FeaturedContent();
        content.setTargetType(dto.getTargetType());
        content.setTargetId(dto.getTargetId());
        content.setPosition(dto.getPosition() != null ? dto.getPosition() : "HOME");
        content.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        content.setCuratedBy(adminId);
        content.setStartAt(dto.getStartAt());
        content.setEndAt(dto.getEndAt());

        FeaturedContent saved = featuredContentRepository.save(content);
        adminLogService.log(adminId, "ADD_FEATURED", "FEATURED_CONTENT", saved.getId(),
                "添加推荐: " + dto.getTargetType() + "#" + dto.getTargetId() + " 到 " + dto.getPosition());

        return saved;
    }

    @Transactional
    public void removeFeaturedContent(Long id, Long adminId) {
        FeaturedContent content = featuredContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("推荐内容", "id", id));
        featuredContentRepository.delete(content);

        adminLogService.log(adminId, "REMOVE_FEATURED", "FEATURED_CONTENT", id,
                "移除推荐: " + content.getTargetType() + "#" + content.getTargetId());
    }

    /**
     * 公开接口：获取某位置的推荐内容列表
     */
    public List<FeaturedContent> getPublicFeatured(String position) {
        return featuredContentRepository.findByPositionOrderBySortOrderDesc(position);
    }
}
