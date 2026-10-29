package com.zhiguang.repository;

import com.zhiguang.entity.ColumnArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColumnArticleRepository extends JpaRepository<ColumnArticle, ColumnArticle.ColumnArticleId> {

    Page<ColumnArticle> findByColumnIdOrderBySortOrderAsc(Long columnId, Pageable pageable);

    List<ColumnArticle> findByColumnId(Long columnId);

    void deleteByColumnIdAndArticleId(Long columnId, Long articleId);

    boolean existsByColumnIdAndArticleId(Long columnId, Long articleId);

    int countByColumnId(Long columnId);
}
