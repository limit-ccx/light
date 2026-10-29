package com.zhiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** 管理端 RAG 知识文档（原文存 MySQL，分块向量存 pgvector，见 AdminKbRagService）。 */
@Entity
@Table(name = "admin_kb_docs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminKbDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    /** MANUAL=操作手册 POLICY=审核政策 METRIC=数据口径 FAQ */
    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType = "MANUAL";

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    /** DRAFT / INDEXED / FAILED */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "created_by")
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
