package com.zhiguang.service;

import com.zhiguang.dto.*;
import com.zhiguang.entity.Announcement;
import com.zhiguang.entity.Banner;
import com.zhiguang.entity.SystemConfig;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.AnnouncementRepository;
import com.zhiguang.repository.BannerRepository;
import com.zhiguang.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final AnnouncementRepository announcementRepository;
    private final BannerRepository bannerRepository;
    private final AdminLogService adminLogService;

    /**
     * 允许管理员修改的系统配置 key 白名单
     */
    private static final Set<String> ALLOWED_CONFIG_KEYS = Set.of(
            "site_name", "site_description", "site_logo",
            "registration_enabled", "email_verification_required",
            "content_review_enabled", "comment_review_enabled",
            "tree_hole_enabled", "tree_hole_review_enabled",
            "ai_summary_enabled", "ai_chat_enabled",
            "max_article_length", "max_comment_length",
            "upload_size_limit",
            "maintenance_mode", "maintenance_message",
            "footer_text", "icp_record",
            "seo_keywords", "seo_description"
    );

    // ========== 系统配置 ==========

    public List<SystemConfigDto> getAllConfigs() {
        return systemConfigRepository.findAll().stream()
                .map(c -> new SystemConfigDto(c.getConfigKey(), c.getConfigValue(), c.getDescription(), c.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public void updateConfig(String key, String value, Long adminId) {
        // 校验 key 是否在白名单中
        if (!ALLOWED_CONFIG_KEYS.contains(key)) {
            throw new BadRequestException("不允许修改的配置项: " + key);
        }

        SystemConfig config = systemConfigRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("配置项", "key", key));

        String oldValue = config.getConfigValue();
        config.setConfigValue(value);
        systemConfigRepository.save(config);

        adminLogService.log(adminId, "UPDATE_CONFIG", "SYSTEM_CONFIG", null,
                "修改配置 " + key + ": " + oldValue + " -> " + value);
    }

    public String getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    /**
     * 批量更新配置
     */
    @Transactional
    public void batchUpdateConfigs(Map<String, String> configs, Long adminId) {
        // 过滤掉不在白名单中的 key
        configs.forEach((key, value) -> {
            if (!ALLOWED_CONFIG_KEYS.contains(key)) {
                throw new BadRequestException("不允许修改的配置项: " + key);
            }
            systemConfigRepository.findById(key).ifPresent(config -> {
                config.setConfigValue(value);
                systemConfigRepository.save(config);
            });
        });
        adminLogService.log(adminId, "BATCH_UPDATE_CONFIG", "SYSTEM_CONFIG", null,
                "批量更新 " + configs.size() + " 个配置项");
    }

    // ========== 公告管理 ==========

    public PageResponse<Announcement> getAnnouncements(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Announcement> announcementPage = (status != null && !status.isBlank())
                ? announcementRepository.findByStatus(status, pageable)
                : announcementRepository.findAll(pageable);

        return new PageResponse<>(announcementPage.getContent(), announcementPage.getTotalElements(),
                announcementPage.getNumber(), announcementPage.getSize(), announcementPage.getTotalPages());
    }

    @Transactional
    public Announcement createAnnouncement(AnnouncementRequestDto dto, Long adminId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setType(dto.getType() != null ? dto.getType() : "NOTICE");
        announcement.setPublishAt(dto.getPublishAt());
        announcement.setStatus("PUBLISHED");

        Announcement saved = announcementRepository.save(announcement);
        adminLogService.log(adminId, "CREATE_ANNOUNCEMENT", "ANNOUNCEMENT", saved.getId(),
                "发布公告: " + dto.getTitle());

        return saved;
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, AnnouncementRequestDto dto, Long adminId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告", "id", id));

        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setType(dto.getType());

        Announcement saved = announcementRepository.save(announcement);
        adminLogService.log(adminId, "UPDATE_ANNOUNCEMENT", "ANNOUNCEMENT", id,
                "更新公告: " + dto.getTitle());

        return saved;
    }

    @Transactional
    public void deleteAnnouncement(Long id, Long adminId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告", "id", id));
        announcementRepository.delete(announcement);

        adminLogService.log(adminId, "DELETE_ANNOUNCEMENT", "ANNOUNCEMENT", id,
                "删除公告: " + announcement.getTitle());
    }

    /**
     * 公开接口：获取已发布公告
     */
    public List<Announcement> getPublishedAnnouncements() {
        return announcementRepository.findByStatusOrderByPublishAtDesc("PUBLISHED");
    }

    // ========== Banner 管理 ==========

    public PageResponse<Banner> getBanners(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Banner> bannerPage = (status != null && !status.isBlank())
                ? bannerRepository.findByStatus(status, pageable)
                : bannerRepository.findAll(pageable);

        return new PageResponse<>(bannerPage.getContent(), bannerPage.getTotalElements(),
                bannerPage.getNumber(), bannerPage.getSize(), bannerPage.getTotalPages());
    }

    @Transactional
    public Banner createBanner(BannerRequestDto dto, Long adminId) {
        Banner banner = new Banner();
        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setPosition(dto.getPosition() != null ? dto.getPosition() : "HOME");
        banner.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        banner.setStatus("ACTIVE");

        Banner saved = bannerRepository.save(banner);
        adminLogService.log(adminId, "CREATE_BANNER", "BANNER", saved.getId(),
                "创建Banner: " + dto.getTitle());

        return saved;
    }

    @Transactional
    public Banner updateBanner(Long id, BannerRequestDto dto, Long adminId) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", "id", id));

        banner.setTitle(dto.getTitle());
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setPosition(dto.getPosition());
        banner.setSortOrder(dto.getSortOrder());

        Banner saved = bannerRepository.save(banner);
        adminLogService.log(adminId, "UPDATE_BANNER", "BANNER", id,
                "更新Banner: " + dto.getTitle());

        return saved;
    }

    @Transactional
    public void updateBannerStatus(Long id, String status, Long adminId) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", "id", id));
        banner.setStatus(status);
        bannerRepository.save(banner);

        adminLogService.log(adminId, "UPDATE_BANNER_STATUS", "BANNER", id,
                "Banner状态变更: " + status);
    }

    @Transactional
    public void deleteBanner(Long id, Long adminId) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", "id", id));
        bannerRepository.delete(banner);

        adminLogService.log(adminId, "DELETE_BANNER", "BANNER", id,
                "删除Banner: " + banner.getTitle());
    }

    /**
     * 公开接口：获取启用的 Banner
     */
    public List<Banner> getActiveBanners(String position) {
        return bannerRepository.findByStatusAndPositionOrderBySortOrderDesc("ACTIVE",
                position != null ? position : "HOME");
    }
}
