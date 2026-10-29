package com.zhiguang.service;

import com.zhiguang.config.AiConfig;
import com.zhiguang.dto.KnowledgeItemDto;
import com.zhiguang.dto.RagAnswerDto;
import com.zhiguang.entity.KnowledgeItem;
import com.zhiguang.entity.RagChatHistory;
import com.zhiguang.entity.User;
import com.zhiguang.repository.KnowledgeItemRepository;
import com.zhiguang.repository.RagChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final RagChatHistoryRepository ragChatHistoryRepository;
    private final AiConfig aiConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * 基于知识库进行问答（简化版RAG）
     * 使用MySQL全文检索 + LLM生成回答
     */
    public RagAnswerDto askKnowledge(Long userId, String question, List<Long> itemIds) {
        log.info("用户 {} 发起知识库问答: {}, 选中文章: {}", userId, question, itemIds);

        try {
            // 1. 检索相关文章
            List<KnowledgeItem> relevantItems;
            if (itemIds != null && !itemIds.isEmpty()) {
                // 使用用户选中的文章
                relevantItems = knowledgeItemRepository.findByIdInAndUserId(itemIds, userId);
            } else {
                // 自动检索相关文章
                relevantItems = retrieveRelevantItems(userId, question);
            }

            // 2. 构建上下文
            String context = buildContext(relevantItems);

            // 3. 调用LLM生成回答
            String answer = generateAnswer(question, context);

            // 4. 保存问答历史
            saveChatHistory(userId, question, answer, relevantItems);

            // 5. 转换为DTO返回
            List<KnowledgeItemDto> sourceDtos = relevantItems.stream()
                    .map(this::toItemDto)
                    .collect(Collectors.toList());

            return new RagAnswerDto(answer, sourceDtos);
        } catch (Exception e) {
            log.error("知识库问答处理失败", e);
            return new RagAnswerDto("抱歉，AI问答处理过程中出现异常，请稍后重试。", new ArrayList<>());
        }
    }

    /**
     * 检索相关知识库条目
     * 使用关键词匹配 + 标题匹配（不依赖 FULLTEXT 索引）
     */
    private List<KnowledgeItem> retrieveRelevantItems(Long userId, String question) {
        List<KnowledgeItem> items = new ArrayList<>();

        // 1. 尝试全文检索（如果 FULLTEXT 索引存在）
        try {
            List<KnowledgeItem> ftItems = knowledgeItemRepository.searchByKeyword(userId, question);
            if (ftItems != null) {
                items.addAll(ftItems);
            }
        } catch (Exception e) {
            log.warn("全文检索不可用，降级为关键词匹配: {}", e.getMessage());
        }

        // 2. 补充按标题/内容关键词匹配
        if (items.size() < 5) {
            List<KnowledgeItem> allItems = knowledgeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
            String lowerQuestion = question.toLowerCase();

            for (KnowledgeItem item : allItems) {
                if (items.contains(item)) continue;

                boolean matched = false;

                // 标题包含问题中的关键词，或问题包含标题关键词
                if (item.getCachedTitle() != null) {
                    String lowerTitle = item.getCachedTitle().toLowerCase();
                    if (lowerQuestion.contains(lowerTitle) ||
                        containsAnyKeyword(item.getCachedTitle(), question)) {
                        matched = true;
                    }
                }

                // 摘要包含问题中的关键词
                if (!matched && item.getCachedSummary() != null &&
                    containsAnyKeyword(item.getCachedSummary(), question)) {
                    matched = true;
                }

                if (matched) {
                    items.add(item);
                }

                if (items.size() >= 5) break;
            }
        }

        // 限制返回数量
        return items.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 检查文本是否包含问题中的任意关键词（按空格和常见标点分词）
     */
    private boolean containsAnyKeyword(String text, String question) {
        if (text == null || question == null) return false;
        String lowerText = text.toLowerCase();
        String[] keywords = question.split("[\\s,;:.?!]+");

        for (String keyword : keywords) {
            String trimmed = keyword.trim();
            if (trimmed.length() > 1 && lowerText.contains(trimmed.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<KnowledgeItem> items) {
        if (items.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            KnowledgeItem item = items.get(i);
            context.append("【文档").append(i + 1).append("】\n");
            context.append("标题：").append(item.getCachedTitle()).append("\n");

            // 优先使用摘要
            if (item.getCachedSummary() != null && !item.getCachedSummary().isBlank()) {
                context.append("摘要：").append(item.getCachedSummary()).append("\n");
            }

            // 添加内容片段（限制长度）
            if (item.getCachedContent() != null) {
                String content = stripHtml(item.getCachedContent());
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                context.append("内容：").append(content).append("\n");
            }

            // 添加个人笔记
            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                context.append("笔记：").append(item.getNotes()).append("\n");
            }

            context.append("\n");
        }

        return context.toString();
    }

    /**
     * 调用LLM生成回答
     */
    private String generateAnswer(String question, String context) {
        String systemPrompt = buildSystemPrompt(context);

        try {
            // 复用AiService的调用方式
            return callLLM(systemPrompt, question);
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            return "抱歉，AI服务暂时不可用，请稍后重试。";
        }
    }

    private String buildSystemPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的知识库助手。请基于以下知识库内容回答用户的问题。\n\n");

        if (context.isEmpty()) {
            prompt.append("当前知识库中没有相关内容。请诚实告知用户。\n\n");
        } else {
            prompt.append("知识库内容：\n").append(context).append("\n");
        }

        prompt.append("回答要求：\n");
        prompt.append("1. 基于知识库内容准确回答问题\n");
        prompt.append("2. 如果知识库中没有相关信息，诚实告知用户\n");
        prompt.append("3. 回答简洁明了，引用相关文档编号（如：根据【文档1】...）\n");
        prompt.append("4. 如果多个文档相关，综合分析回答\n");
        prompt.append("5. 回答控制在300字以内\n");

        return prompt.toString();
    }

    private String callLLM(String systemPrompt, String userQuestion) {
        // 构建 OpenAI 兼容格式请求体
        Map<String, Object> requestBody = Map.of(
            "model", aiConfig.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userQuestion)
            ),
            "temperature", 0.5,
            "max_tokens", 1000
        );

        WebClient webClient = webClientBuilder
            .baseUrl(aiConfig.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
            .block();

        return extractContent(response);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String getApiKey() {
        // 从配置获取
        String apiKey = aiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            // 尝试从环境变量获取
            apiKey = System.getenv("DEEPSEEK_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("DeepSeek API Key 未配置");
        }
        return apiKey;
    }

    private void saveChatHistory(Long userId, String question, String answer, List<KnowledgeItem> sources) {
        try {
            RagChatHistory history = new RagChatHistory();
            history.setUser(new User() {{ setId(userId); }});
            history.setQuestion(question);
            history.setAnswer(answer);

            // 保存引用来源ID
            List<Long> sourceIds = sources.stream()
                    .map(KnowledgeItem::getId)
                    .collect(Collectors.toList());
            history.setSources(sourceIds.toString());

            ragChatHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("保存问答历史失败", e);
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&amp;", "&")
                   .trim();
    }

    private KnowledgeItemDto toItemDto(KnowledgeItem item) {
        return new KnowledgeItemDto(
                item.getId(),
                item.getCachedTitle(),
                null, // 不返回完整内容
                item.getCachedSummary(),
                item.getCachedCover(),
                null,
                item.getCachedAuthorName(),
                item.getCachedAuthorAvatar(),
                item.getOriginalArticleId(),
                item.getOriginalDeleted(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                null,
                item.getReadStatus() != null ? item.getReadStatus().name() : "UNREAD",
                item.getCustomTags(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
