package com.zhiguang.repository;

import com.zhiguang.entity.ArticleCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends JpaRepository<ArticleCollection, Long> {

    Page<ArticleCollection> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
