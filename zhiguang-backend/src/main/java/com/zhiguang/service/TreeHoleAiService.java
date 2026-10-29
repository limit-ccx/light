package com.zhiguang.service;

import com.zhiguang.config.AiConfig;
import com.zhiguang.entity.TreeHoleReply;
import com.zhiguang.repository.TreeHolePostRepository;
import com.zhiguang.repository.TreeHoleReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeHoleAiService {

    private final AiConfig aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final TreeHoleReplyRepository replyRepository;
    private final TreeHolePostRepository postRepository;

    private static final String AI_HASH = "AI_COMPANION";

    private static final Set<String> NEGATIVE_EMOTIONS = Set.of(
            "sadness", "anxiety", "loneliness", "anger"
    );

    private static final String SYSTEM_PROMPT = """
            你是"纸域空间小暖"，一个温暖、善解人意的AI伙伴。
            一位匿名用户正在树洞中倾诉心事，请用共情、温暖的方式回应ta。
            要求：
            - 先表达理解和共情，不要急于给建议
            - 不要说教，不要评判
            - 语气亲切自然，像一个知心朋友
            - 回应简短，控制在100字以内
            - 如果对方表达了严重的心理危机（如自杀倾向），请温和地建议寻求专业帮助，并提供心理援助热线：400-161-9995
            """;

    public boolean shouldGenerateResponse(String emotionTag) {
        return emotionTag != null && NEGATIVE_EMOTIONS.contains(emotionTag.toLowerCase());
    }

    @Async
    public void generateWarmResponseAsync(Long postId, String postContent, String emotionTag) {
        try {
            String response = callDeepSeek(postContent, emotionTag);
            if (response != null && !response.isBlank()) {
                TreeHoleReply reply = new TreeHoleReply();
                reply.setPostId(postId);
                reply.setAnonymousHash(AI_HASH);
                reply.setContent(response.trim());
                reply.setStatus(1);
                reply.setIsAiGenerated(true);
                replyRepository.save(reply);
                postRepository.incrementReplyCount(postId);
                log.info("AI 暖心回应生成成功，帖子ID: {}", postId);
            }
        } catch (Exception e) {
            log.error("AI 暖心回应生成失败，帖子ID: {}", postId, e);
        }
    }

    private String callDeepSeek(String postContent, String emotionTag) {
        String emotionLabel = switch (emotionTag) {
            case "sadness" -> "难过";
            case "anxiety" -> "焦虑";
            case "loneliness" -> "孤独";
            case "anger" -> "愤怒";
            default -> "低落";
        };

        String userMessage = String.format("倾诉者此刻的心情标签是「%s」。ta说：\n\n%s", emotionLabel, postContent);

        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.85,
                "max_tokens", 200
        );

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(aiConfig.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiConfig.getApiKey())
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

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
        }
        return null;
    }
}
