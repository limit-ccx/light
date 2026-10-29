package com.zhiguang.repository;

import com.zhiguang.entity.CollectionArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionArticleRepository extends JpaRepository<CollectionArticle, CollectionArticle.CollectionArticleId> {

    Page<CollectionArticle> findByCollectionIdOrderBySortOrderAsc(Long collectionId, Pageable pageable);

    void deleteByCollectionIdAndArticleId(Long collectionId, Long articleId);

    boolean existsByCollectionIdAndArticleId(Long collectionId, Long articleId);

    int countByCollectionId(Long collectionId);
}
