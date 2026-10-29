package com.zhiguang.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class DocumentParserService {

    private final Tika tika = new Tika();

    /**
     * 解析上传的文件，提取文本内容
     */
    public ParsedDocument parse(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long size = file.getSize();

            log.info("解析文档: {}, 类型: {}, 大小: {} bytes", originalFilename, contentType, size);

            // 使用 Tika 解析文档
            String content = extractText(file.getInputStream());

            // 生成标题（使用文件名去掉扩展名）
            String title = generateTitle(originalFilename);

            // 生成摘要（取前200字）
            String summary = generateSummary(content);

            return new ParsedDocument(title, content, summary, originalFilename, contentType);
        } catch (Exception e) {
            log.error("文档解析失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    private String extractText(InputStream inputStream) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        // 限制提取文本上限：10MB 的压缩包/PDF 可解压出上百 MB 文本，
        // 无限制（-1）时数个并发导入即可 OOM
        BodyContentHandler handler = new BodyContentHandler(1_000_000);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        parser.parse(inputStream, handler, metadata, context);
        return handler.toString().trim();
    }

    private String generateTitle(String filename) {
        if (filename == null || filename.isBlank()) {
            return "未命名文档";
        }
        // 去掉扩展名
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(0, lastDot);
        }
        return filename;
    }

    private String generateSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        // 取前300字作为摘要
        String cleaned = content.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 300) {
            return cleaned.substring(0, 300) + "...";
        }
        return cleaned;
    }

    /**
     * 检查文件类型是否支持
     */
    public boolean isSupported(String contentType, String filename) {
        if (contentType == null && filename == null) {
            return false;
        }

        // 支持的 MIME 类型
        String[] supportedTypes = {
            "text/plain",
            "text/markdown",
            "text/html",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        };

        if (contentType != null) {
            for (String type : supportedTypes) {
                if (contentType.contains(type)) {
                    return true;
                }
            }
        }

        // 通过后缀名判断
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".txt") ||
                   lower.endsWith(".md") ||
                   lower.endsWith(".markdown") ||
                   lower.endsWith(".pdf") ||
                   lower.endsWith(".doc") ||
                   lower.endsWith(".docx") ||
                   lower.endsWith(".xls") ||
                   lower.endsWith(".xlsx") ||
                   lower.endsWith(".ppt") ||
                   lower.endsWith(".pptx") ||
                   lower.endsWith(".html") ||
                   lower.endsWith(".htm");
        }

        return false;
    }

    public record ParsedDocument(
        String title,
        String content,
        String summary,
        String originalFilename,
        String contentType
    ) {}
}
