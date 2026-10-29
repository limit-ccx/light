package com.zhiguang.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AnonymousIdentityService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String[] ADJECTIVES = {
            "温柔的", "勇敢的", "安静的", "明亮的", "温暖的",
            "自由的", "坚定的", "柔软的", "闪耀的", "沉稳的",
            "灵动的", "恬静的", "热烈的", "从容的", "细腻的"
    };

    private static final String[] ANIMALS = {
            "月光兔🐰", "小熊猫🦊", "星空猫🐱", "森林鹿🦌", "海洋鲸🐋",
            "云朵羊🐑", "山谷鹰🦅", "花园蜂🐝", "雪地熊🐻", "晨曦鸟🐦",
            "彩虹蝶🦋", "溪水鱼🐟", "暮色狼🐺", "朝阳凤🐦", "竹林鹤🕊️"
    };

    private static final String[] COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
            "#F1948A", "#82E0AA", "#F8C471", "#AED6F1", "#D2B4DE"
    };

    @SuppressWarnings("unchecked")
    public Map<String, String> generateIdentity(String anonymousHash) {
        if (anonymousHash == null || anonymousHash.isBlank()) {
            return Map.of("displayName", "匿名旅人", "emoji", "🌟", "color", "#94a3b8");
        }

        String cacheKey = "anon:id:" + anonymousHash;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                String[] parts = cached.split("\\|");
                if (parts.length == 3) {
                    return Map.of("displayName", parts[0], "emoji", parts[1], "color", parts[2]);
                }
            }
        } catch (Exception ignored) {
            // Redis 不可用时降级
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(anonymousHash.getBytes(StandardCharsets.UTF_8));

            int adjIndex = Math.abs((hash[0] & 0xFF) + (hash[1] & 0xFF)) % ADJECTIVES.length;
            int aniIndex = Math.abs((hash[2] & 0xFF) + (hash[3] & 0xFF)) % ANIMALS.length;
            int colIndex = Math.abs((hash[4] & 0xFF) + (hash[5] & 0xFF)) % COLORS.length;

            String displayName = ADJECTIVES[adjIndex] + ANIMALS[aniIndex].split("[\\p{Emoji}]")[0];
            String emoji = extractEmoji(ANIMALS[aniIndex]);
            String color = COLORS[colIndex];

            try {
                stringRedisTemplate.opsForValue().set(cacheKey,
                        displayName + "|" + emoji + "|" + color, 24, TimeUnit.HOURS);
            } catch (Exception ignored) {}

            return Map.of("displayName", displayName, "emoji", emoji, "color", color);
        } catch (Exception e) {
            return Map.of("displayName", "匿名旅人", "emoji", "🌟", "color", "#94a3b8");
        }
    }

    private String extractEmoji(String text) {
        // Extract emoji from strings like "月光兔🐰"
        return text.replaceAll("[^\\p{Emoji_Presentation}\\p{Emoji}\\uFE0F]", "");
    }
}
