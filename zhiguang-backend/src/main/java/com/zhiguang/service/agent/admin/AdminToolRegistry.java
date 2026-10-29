package com.zhiguang.service.agent.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 管理侧工具白名单注册表（独立于 C 端 AgentToolRegistry）。
 * <p>
 * L3 签名去重的归一化工具方法也在此提供（编排层使用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminToolRegistry {

    private final List<AdminAgentTool> tools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, AdminAgentTool> registry;

    @PostConstruct
    public void init() {
        registry = new LinkedHashMap<>();
        for (AdminAgentTool tool : tools) {
            registry.put(tool.name(), tool);
        }
        log.info("Admin Agent 工具注册完成({} 个，写操作 {} 个): {}",
                registry.size(),
                registry.values().stream().filter(AdminAgentTool::write).count(),
                registry.keySet());
    }

    private static final Map<String, Object> ASK_USER_DEF = Map.of(
            "type", "function",
            "function", Map.of(
                    "name", "ask_user",
                    "description", "向管理员征询意见（B级处置必用）：边缘内容、证据不足、影响面大、需裁量权衡时调用。"
                            + "给出事实摘要、你的建议倾向与依据、可选项。调用后本轮结束，等待管理员答复。",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "question", Map.of("type", "string", "description", "征询问题（含事实摘要）"),
                                    "suggestion", Map.of("type", "string", "description", "你的建议倾向与依据"),
                                    "options", Map.of("type", "array",
                                            "items", Map.of("type", "string"),
                                            "description", "可选项列表")),
                            "required", java.util.List.of("question"))));

    public List<Map<String, Object>> toToolDefinitions() {
        java.util.List<Map<String, Object>> defs = new java.util.ArrayList<>();
        defs.add(ASK_USER_DEF); // 引擎内建工具（拦截型，无 bean 实现）
        registry.values().stream()
                .map(tool -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.parametersSchema())))
                .forEach(defs::add);
        return defs;
    }

    public AdminAgentTool get(String name) {
        return registry.get(name);
    }

    public boolean isBuiltin(String name) {
        return "ask_user".equals(name);
    }

    /** L3：参数归一化签名（key 排序），相同签名在一次请求内只真实执行一次 */
    public String signature(String toolName, String argumentsJson) {
        try {
            Map<String, Object> args = argumentsJson == null || argumentsJson.isBlank()
                    ? Map.of() : objectMapper.readValue(argumentsJson, Map.class);
            Map<String, Object> sorted = new TreeMap<>(args);
            return toolName + ":" + objectMapper.writeValueAsString(sorted);
        } catch (Exception e) {
            return toolName + ":" + argumentsJson;
        }
    }
}
