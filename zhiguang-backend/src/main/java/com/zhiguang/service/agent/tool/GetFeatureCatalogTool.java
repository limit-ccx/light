package com.zhiguang.service.agent;

import com.zhiguang.entity.AgentFeatureKb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台功能目录查询：基于《知光客户端功能指南》知识库，
 * 返回功能名称/简介/使用要点（不含页面路径）。
 */
@Component
@RequiredArgsConstructor
public class GetFeatureCatalogTool implements AgentTool {

    private final AgentKbService kbService;

    @Override
    public String name() {
        return "get_feature_catalog";
    }

    @Override
    public String description() {
        return "查询纸域空间平台的功能目录与使用要点。当用户问'平台有什么功能''XX是什么''XX怎么用''有什么规则'时调用。可选传关键词缩小范围。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "keyword", Map.of(
                                "type", "string",
                                "description", "可选。功能关键词，如：私信、树洞、润色、签到、星图")),
                "required", List.of());
    }

    @Override
    public String execute(Long userId, Map<String, Object> args) throws Exception {
        String keyword = args.get("keyword") == null ? null : String.valueOf(args.get("keyword"));
        List<AgentFeatureKb> entries = kbService.search(keyword);
        List<Map<String, Object>> features = entries.stream()
                .map(kb -> Map.<String, Object>of(
                        "title", kb.getTitle(),
                        "summary", kb.getSummary(),
                        "detail", kb.getDetail() == null ? "" : kb.getDetail()))
                .collect(Collectors.toList());
        return "{\"features\":" + new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(features) + "}";
    }
}
