package com.zhiguang.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 调用指标（Micrometer/Prometheus）。
 * <p>
 * - ai_calls_total{endpoint,result}：调用量与结果分布（success/truncated/error/cancelled）
 * - ai_tokens_total{type=prompt|completion,endpoint}：token 消耗（成本监控）
 * - ai_latency{endpoint}：耗时分布（Grafana 中以 ai_latency_seconds 呈现）
 * 指标记录失败绝不影响业务调用。
 */
@Component
@RequiredArgsConstructor
public class AiMetrics {

    private final MeterRegistry meterRegistry;

    public void recordCall(String endpoint, String result, long latencyMs) {
        try {
            meterRegistry.counter("ai_calls_total", "endpoint", endpoint, "result", result).increment();
            meterRegistry.timer("ai_latency", "endpoint", endpoint).record(latencyMs, TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
        }
    }

    public void recordTokens(String endpoint, Integer promptTokens, Integer completionTokens) {
        try {
            if (promptTokens != null && promptTokens > 0) {
                meterRegistry.counter("ai_tokens_total", "type", "prompt", "endpoint", endpoint).increment(promptTokens);
            }
            if (completionTokens != null && completionTokens > 0) {
                meterRegistry.counter("ai_tokens_total", "type", "completion", "endpoint", endpoint).increment(completionTokens);
            }
        } catch (Exception ignore) {
        }
    }
}
