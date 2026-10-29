package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 导览助手功能知识库条目。
 * <p>
 * 源文档为《知光客户端功能指南》（doc/知光客户端功能指南.md），
 * 由运营拆条录入；不含页面路由字段（agent 不做页面导航）。
 */
@Entity
@Table(name = "agent_feature_kb", indexes = {
        @Index(name = "idx_kb_enabled_sort", columnList = "enabled, sort_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentFeatureKb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 功能名，如「专栏」 */
    @Column(nullable = false, length = 50)
    private String title;

    /** 一句话说明 */
    @Column(nullable = false, length = 200)
    private String summary;

    /** 命中关键词，逗号分隔 */
    @Column(length = 200)
    private String keywords;

    /** 使用要点/规则边界（源自功能指南） */
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
