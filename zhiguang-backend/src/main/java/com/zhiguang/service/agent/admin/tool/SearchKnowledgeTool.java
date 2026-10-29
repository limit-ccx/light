package com.zhiguang.service.agent.admin.tool;

import com.zhiguang.service.agent.admin.AdminAgentTool;
import com.zhiguang.service.agent.admin.AdminKbRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** RAG 向量知识检索（设计方案 §3）：运营文档/审核政策/数据口径问答的唯一事实源。 */
@Component
@RequiredArgsConstructor
public class SearchKnowledgeTool implements AdminAgentTool {

    private final AdminKbRagService ragService;

    @Override
    public String name() {
        return "search_knowledge";
    }

    @Override
    public String description() {
        return "检索运营知识库（操作手册/审核政策/数据口径/FAQ），返回带来源的文档片段。涉及平台功能、审核标准、指标口径、操作规范的问题都应先调用本工具。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "检索问题或关键词")),
                "required", List.of("query"));
    }

    @Override
    public String execute(Long adminId, Map<String, Object> args) throws Exception {
        String query = String.valueOf(args.getOrDefault("query", ""));
        List<Map<String, Object>> hits = ragService.search(query);
        if (hits.isEmpty()) {
            // 明确的不命中信号（§13.2）：模型据此回答"没查到"，禁止编造
            return "{\"hit\":0,\"message\":\"知识库未命中，请如实告知未找到相关文档\"}";
        }
        StringBuilder sb = new StringBuilder("{\"hit\":").append(hits.size()).append(",\"fragments\":[");
        for (int i = 0; i < hits.size(); i++) {
            var hit = hits.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"source\":\"").append(hit.get("title")).append("\",\"score\":")
                    .append(hit.get("score")).append(",\"content\":\"")
                    .append(escape(String.valueOf(hit.get("content")))).append("\"}");
        }
        return sb.append("]}").toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
