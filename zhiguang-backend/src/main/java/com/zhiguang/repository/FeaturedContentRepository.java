package com.zhiguang.repository;

import com.zhiguang.entity.FeaturedContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeaturedContentRepository extends JpaRepository<FeaturedContent, Long> {

    Page<FeaturedContent> findByPosition(String position, Pageable pageable);

    List<FeaturedContent> findByPositionOrderBySortOrderDesc(String position);

    boolean existsByTargetTypeAndTargetIdAndPosition(String targetType, Long targetId, String position);
}
