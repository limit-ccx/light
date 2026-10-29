package com.zhiguang.service;

import com.zhiguang.dto.ArticleVersionDto;
import com.zhiguang.entity.Article;
import com.zhiguang.entity.ArticleVersion;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.ArticleRepository;
import com.zhiguang.repository.ArticleVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleVersionService {

    private final ArticleVersionRepository versionRepository;
    private final ArticleRepository articleRepository;

    /** 自动保存版本（编辑文章时调用） */
    @Transactional
    public void saveVersion(Article article) {
        int count = versionRepository.countByArticleId(article.getId());
        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setVersion(count + 1);
        versionRepository.save(version);
        log.debug("文章 id={} 版本 v{} 已保存", article.getId(), version.getVersion());
    }

    /** 获取文章所有历史版本 */
    public List<ArticleVersionDto> getVersions(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", articleId));
        if (!article.getAuthor().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权查看版本历史");
        }
        return versionRepository.findByArticleIdOrderByVersionDesc(articleId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /** 获取单个版本详情 */
    public ArticleVersionDto getVersion(Long versionId, Long userId) {
        ArticleVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("版本", "id", versionId));
        Article article = articleRepository.findById(version.getArticleId())
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", version.getArticleId()));
        if (!article.getAuthor().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权查看此版本");
        }
        return toDto(version);
    }

    /** 恢复到指定版本 */
    @Transactional
    public Article restoreVersion(Long articleId, Long versionId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", articleId));
        if (!article.getAuthor().getId().equals(userId)) {
            throw new ResourceNotFoundException("无权恢复版本");
        }
        ArticleVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("版本", "id", versionId));
        // 先保存当前版本，再恢复
        saveVersion(article);
        article.setTitle(version.getTitle());
        article.setContent(version.getContent());
        articleRepository.save(article);
        return article;
    }

    private ArticleVersionDto toDto(ArticleVersion v) {
        return new ArticleVersionDto(
                v.getId(), v.getArticleId(), v.getTitle(), v.getContent(),
                v.getVersion(), v.getCreatedAt());
    }
}
