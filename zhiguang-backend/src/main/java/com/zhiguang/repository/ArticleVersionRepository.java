package com.zhiguang.repository;

import com.zhiguang.entity.ArticleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Long> {

    List<ArticleVersion> findByArticleIdOrderByVersionDesc(Long articleId);

    int countByArticleId(Long articleId);
}
