package com.zhiguang.repository;

import com.zhiguang.entity.KnowledgeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {

    Page<KnowledgeItem> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<KnowledgeItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<KnowledgeItem> findByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, Long categoryId, Pageable pageable);

    Optional<KnowledgeItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndOriginalArticleId(Long userId, Long originalArticleId);

    @Query("SELECT ki FROM KnowledgeItem ki WHERE ki.user.id = :userId AND " +
            "(LOWER(ki.cachedTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(ki.cachedContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(ki.cachedSummary) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
            "ORDER BY ki.createdAt DESC")
    List<KnowledgeItem> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("SELECT DISTINCT tag FROM KnowledgeItem ki JOIN ki.customTags tag WHERE ki.user.id = :userId")
    List<String> findAllTagsByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndReadStatus(Long userId, KnowledgeItem.ReadStatus readStatus);

    List<KnowledgeItem> findByIdInAndUserId(List<Long> ids, Long userId);
}
