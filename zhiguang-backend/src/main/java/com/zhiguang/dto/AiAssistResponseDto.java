package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 一键生成结果：一次请求同时产出标题候选、摘要与标签。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAssistResponseDto {

    /** 标题候选（3-5 个） */
    private List<String> titles;

    /** 摘要（80-150 字） */
    private String summary;

    /** 标签（3-8 个） */
    private List<String> tags;
}
