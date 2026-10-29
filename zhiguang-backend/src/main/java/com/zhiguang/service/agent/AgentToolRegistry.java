package com.zhiguang.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.AgentConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 工具白名单注册表。
 * <p>
 * LLM 幻觉出的工具名不会执行任何真实逻辑，只会返回错误信息让其改道；
 * 每次执行限时（默认 3s），超时返回错误结果而非中断整次对话。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentToolRegistry {

    private final List<AgentTool> tools;
    private final AgentConfig agentConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, AgentTool> registry;

    @PostConstruct
    public void init() {
        registry = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            registry.put(tool.name(), tool);
        }
        log.info("Agent 工具注册完成: {}", registry.keySet());
    }

    /** OpenAI tools 协议的工具定义列表 */
    public List<Map<String, Object>> toToolDefinitions() {
        return registry.values().stream()
                .map(tool -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.parametersSchema())))
                .toList();
    }

    public boolean has(String name) {
        return registry.containsKey(name);
    }

    /**
     * 执行工具并返回 JSON 字符串结果（含超时与未知工具兜底）。
     */
    public String execute(Long userId, String name, String argumentsJson) {
        AgentTool tool = registry.get(name);
        if (tool == null) {
            return "{\"error\":\"unknown tool\",\"hint\":\"只能调用已注册的工具\"}";
        }
        Map<String, Object> args = parseArgs(argumentsJson);
        try {
            String result = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return tool.execute(userId, args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .get(agentConfig.getToolTimeoutMs(), TimeUnit.MILLISECONDS);
            log.debug("Agent 工具执行成功: tool={}, userId={}", name, userId);
            return result;
        } catch (Exception e) {
            log.warn("Agent 工具执行失败: tool={}, error={}", name, e.getMessage());
            return "{\"error\":\"tool execution failed\"}";
        }
    }

    private Map<String, Object> parseArgs(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
