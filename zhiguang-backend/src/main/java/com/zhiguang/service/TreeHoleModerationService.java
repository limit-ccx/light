package com.zhiguang.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TreeHoleModerationService {

    private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
            "自杀", "自殺", "跳楼", "自残", "杀人", "谋杀", "强奸", "性侵", "色情", "赌博",
            "毒品", "吸毒", "贩毒", "诈骗", "诈骗", "傻逼", "他妈的", "草泥马", "狗日的",
            "bitch", "fuck", "shit", "damn", "kill yourself", "suicide"
    );

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            String.join("|", SENSITIVE_KEYWORDS),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MIN_CONTENT_LENGTH = 5;

    public ModerationResult moderate(String content) {
        if (content == null || content.trim().length() < MIN_CONTENT_LENGTH) {
            return ModerationResult.reject("内容过短，请真诚表达");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            return ModerationResult.reject("内容过长，请控制在 2000 字以内");
        }

        int riskScore = 0;
        if (SENSITIVE_PATTERN.matcher(content).find()) {
            riskScore += 50;
        }

        // 重复字符检测（刷屏）
        if (containsSpamPattern(content)) {
            riskScore += 30;
        }

        // 联系方式检测（防止人肉/诈骗）
        if (containsContactInfo(content)) {
            riskScore += 30;
        }

        if (riskScore >= 60) {
            return ModerationResult.reject("内容包含不当信息，请修改后重试");
        }
        if (riskScore >= 30) {
            return ModerationResult.pending("内容需要人工复核");
        }
        return ModerationResult.approve();
    }

    private boolean containsSpamPattern(String content) {
        if (content.length() < 10) return false;
        java.util.Map<Character, Integer> charCount = new java.util.HashMap<>();
        for (char c : content.toCharArray()) {
            charCount.merge(c, 1, Integer::sum);
        }
        int threshold = (int) (content.length() * 0.5);
        return charCount.values().stream().anyMatch(count -> count > threshold);
    }

    private boolean containsContactInfo(String content) {
        return content.matches(".*(\\d{11}|\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b|微信|QQ|qq|手机号).*");
    }

    public record ModerationResult(boolean approved, boolean pending, String reason) {
        static ModerationResult approve() {
            return new ModerationResult(true, false, null);
        }
        static ModerationResult pending(String reason) {
            return new ModerationResult(false, true, reason);
        }
        static ModerationResult reject(String reason) {
            return new ModerationResult(false, false, reason);
        }
    }
}
