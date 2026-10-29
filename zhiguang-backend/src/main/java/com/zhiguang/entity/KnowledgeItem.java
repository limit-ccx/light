package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "knowledge_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ===== 完整缓存文章内容 =====
    @Column(name = "cached_title", nullable = false, length = 200)
    private String cachedTitle;

    @Column(name = "cached_content", columnDefinition = "LONGTEXT")
    private String cachedContent;

    @Column(name = "cached_summary", length = 500)
    private String cachedSummary;

    @Column(name = "cached_cover", length = 500)
    private String cachedCover;

    @Column(name = "cached_tags", columnDefinition = "JSON")
    private String cachedTags;

    @Column(name = "cached_author_name", length = 50)
    private String cachedAuthorName;

    @Column(name = "cached_author_avatar", length = 255)
    private String cachedAuthorAvatar;

    // ===== 原文章引用（可选，用于追踪） =====
    @Column(name = "original_article_id")
    private Long originalArticleId;

    @Column(name = "original_deleted")
    private Boolean originalDeleted = false;

    // ===== 分类和笔记 =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private KnowledgeCategory category;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ===== 阅读状态 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "read_status")
    private ReadStatus readStatus = ReadStatus.UNREAD;

    // ===== 自定义标签 =====
    @ElementCollection
    @CollectionTable(name = "knowledge_item_tags", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "tag")
    private List<String> customTags = new ArrayList<>();

    // ===== 时间戳 =====
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReadStatus {
        UNREAD, READING, READ
    }
}
