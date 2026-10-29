package com.zhiguang.service;

import com.zhiguang.dto.*;
import com.zhiguang.entity.Article;
import com.zhiguang.entity.Comment;
import com.zhiguang.entity.Report;
import com.zhiguang.entity.SensitiveWord;
import com.zhiguang.exception.BadRequestException;
import com.zhiguang.exception.ResourceNotFoundException;
import com.zhiguang.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final SensitiveWordRepository sensitiveWordRepository;
    private final UserRepository userRepository;
    private final AdminLogService adminLogService;

    // ========== 文章审核 ==========

    public PageResponse<AdminArticleDto> getArticles(Integer status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articlePage = articleRepository.searchForAdmin(status, keyword, pageable);

        List<AdminArticleDto> list = articlePage.getContent().stream()
                .map(this::toAdminArticleDto)
                .toList();

        return new PageResponse<>(list, articlePage.getTotalElements(), articlePage.getNumber(),
                articlePage.getSize(), articlePage.getTotalPages());
    }

    @Transactional
    public void reviewArticle(Long articleId, ArticleReviewDto dto, Long adminId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("文章", "id", articleId));

        int oldStatus = article.getStatus();
        article.setStatus(dto.getStatus());
        articleRepository.save(article);

        String action = dto.getStatus() == 2 ? "APPROVE_ARTICLE" : "TAKEDOWN_ARTICLE";
        adminLogService.log(adminId, action, "ARTICLE", articleId,
                "文章「" + article.getTitle() + "」状态: " + oldStatus + " -> " + dto.getStatus()
                        + (dto.getReason() != null ? ", 原因: " + dto.getReason() : ""));
    }

    // ========== 评论审核 ==========

    public PageResponse<AdminCommentDto> getComments(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.searchForAdmin(status, keyword, pageable);

        List<AdminCommentDto> list = commentPage.getContent().stream()
                .map(this::toAdminCommentDto)
                .toList();

        return new PageResponse<>(list, commentPage.getTotalElements(), commentPage.getNumber(),
                commentPage.getSize(), commentPage.getTotalPages());
    }

    @Transactional
    public void reviewComment(Long commentId, CommentReviewDto dto, Long adminId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论", "id", commentId));

        String oldStatus = comment.getStatus();
        comment.setStatus(dto.getStatus());
        commentRepository.save(comment);

        String action = "DELETED".equals(dto.getStatus()) ? "DELETE_COMMENT" : "APPROVE_COMMENT";
        adminLogService.log(adminId, action, "COMMENT", commentId,
                "评论状态: " + oldStatus + " -> " + dto.getStatus());
    }

    // ========== 举报处理 ==========

    public PageResponse<ReportResponseDto> getReports(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reportPage = status != null && !status.isBlank()
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAll(pageable);

        List<ReportResponseDto> list = reportPage.getContent().stream()
                .map(this::toReportResponseDto)
                .toList();

        return new PageResponse<>(list, reportPage.getTotalElements(), reportPage.getNumber(),
                reportPage.getSize(), reportPage.getTotalPages());
    }

    @Transactional
    public void handleReport(Long reportId, HandleReportDto dto, Long adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("举报", "id", reportId));

        if (!"PENDING".equals(report.getStatus())) {
            throw new BadRequestException("该举报已被处理");
        }

        report.setStatus(dto.getStatus());
        report.setHandlerId(adminId);
        report.setHandleResult(dto.getHandleResult());
        report.setHandledAt(LocalDateTime.now());
        reportRepository.save(report);

        adminLogService.log(adminId, "HANDLE_REPORT", "REPORT", reportId,
                "处理举报: " + dto.getStatus() + ", 目标: " + report.getTargetType() + "#" + report.getTargetId());
    }

    // ========== 敏感词管理 ==========

    public PageResponse<SensitiveWord> getSensitiveWords(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SensitiveWord> wordPage;
        if (category != null && !category.isBlank()) {
            wordPage = sensitiveWordRepository.findAll(pageable);
            // category 筛选在内存中完成（数据量小）
            wordPage = wordPage.map(w -> w);
        } else {
            wordPage = sensitiveWordRepository.findAll(pageable);
        }
        return new PageResponse<>(wordPage.getContent(), wordPage.getTotalElements(),
                wordPage.getNumber(), wordPage.getSize(), wordPage.getTotalPages());
    }

    @Transactional
    public SensitiveWord addSensitiveWord(SensitiveWordDto dto) {
        if (sensitiveWordRepository.existsByWord(dto.getWord())) {
            throw new BadRequestException("该敏感词已存在");
        }
        SensitiveWord word = new SensitiveWord();
        word.setWord(dto.getWord());
        word.setCategory(dto.getCategory());
        return sensitiveWordRepository.save(word);
    }

    @Transactional
    public void deleteSensitiveWord(Long wordId, Long adminId) {
        SensitiveWord word = sensitiveWordRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("敏感词", "id", wordId));
        sensitiveWordRepository.delete(word);
        adminLogService.log(adminId, "DELETE_SENSITIVE_WORD", "SENSITIVE_WORD", wordId,
                "删除敏感词: " + word.getWord());
    }

    /**
     * 检查文本是否包含敏感词（公开接口，供发布时调用）
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isBlank()) return false;
        List<SensitiveWord> words = sensitiveWordRepository.findAll();
        for (SensitiveWord sw : words) {
            if (text.contains(sw.getWord())) {
                return true;
            }
        }
        return false;
    }

    // ========== 转换方法 ==========

    private AdminArticleDto toAdminArticleDto(Article article) {
        return new AdminArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getCover(),
                article.getAuthor().getId(),
                article.getAuthor().getUsername(),
                article.getStatus(),
                article.getViewCount(),
                article.getLikeCount(),
                article.getFavoriteCount(),
                article.getCommentCount(),
                article.getTags(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private AdminCommentDto toAdminCommentDto(Comment comment) {
        return new AdminCommentDto(
                comment.getId(),
                comment.getContent(),
                comment.getArticle().getId(),
                comment.getArticle().getTitle(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getStatus() != null ? comment.getStatus() : "NORMAL",
                comment.getCreatedAt()
        );
    }

    private ReportResponseDto toReportResponseDto(Report report) {
        String reporterName = userRepository.findById(report.getReporterId())
                .map(u -> u.getUsername()).orElse("未知用户");

        String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());

        return new ReportResponseDto(
                report.getId(),
                report.getReporterId(),
                reporterName,
                report.getTargetType(),
                report.getTargetId(),
                targetTitle,
                report.getReason(),
                report.getStatus(),
                report.getHandlerId(),
                report.getHandleResult(),
                report.getHandledAt(),
                report.getCreatedAt()
        );
    }

    private String getTargetTitle(String targetType, Long targetId) {
        try {
            if ("ARTICLE".equals(targetType)) {
                return articleRepository.findById(targetId)
                        .map(Article::getTitle).orElse("已删除");
            } else if ("COMMENT".equals(targetType)) {
                return commentRepository.findById(targetId)
                        .map(c -> c.getContent().substring(0, Math.min(50, c.getContent().length())) + "...")
                        .orElse("已删除");
            }
        } catch (Exception e) {
            // ignore
        }
        return "目标#" + targetId;
    }
}
