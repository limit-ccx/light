package com.zhiguang.exception;

/**
 * AI 输出因超出 max_tokens 上限被截断（finish_reason=length）时抛出。
 * 截断的润色/总结结果若静默返回，会被用户当成完整内容采纳，因此必须显式失败。
 */
public class AiOutputTruncatedException extends RuntimeException {

    public AiOutputTruncatedException() {
        super("AI 输出超出长度上限被截断，请缩短内容后重试");
    }
}
