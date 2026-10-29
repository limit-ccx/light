package com.zhiguang.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContentModerationService {

    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "傻逼", "sb", "他妈的", "tmd", "草泥马", "cnm", "法轮功", "赌博", "色情", "嫖娼", "毒品",
        "吸毒", "贩毒", "暴力", "血腥", "屠杀", "炸弹", " Terrorist ", "恐怖分子", "台独", "藏独",
        "疆独", "反华", "反动", "色情", "淫秽", "卖淫", "嫖娼", "赌博", "博彩", "毒品", "吸毒",
        "冰毒", "海洛因", "可卡因", "大麻", "摇头丸", "K粉", "鸦片", "吗啡", "杜冷丁",
        "fuck", "shit", "bitch", "damn", "ass", "cunt", "dick", "pussy", "nigger"
    ));

    private static final Pattern PATTERN = Pattern.compile("[\\s\\p{Punct}]*");

    public boolean containsSensitiveWords(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toLowerCase().replaceAll("[\\s\\p{Punct}]+", "");
        for (String word : SENSITIVE_WORDS) {
            if (normalized.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void validate(String content) {
        if (containsSensitiveWords(content)) {
            throw new IllegalArgumentException("内容包含敏感词汇，请修改后重新提交");
        }
    }
}
