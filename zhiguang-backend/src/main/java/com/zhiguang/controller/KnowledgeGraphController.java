package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.entity.KnowledgeNode;
import com.zhiguang.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService graphService;

    /** 从文章提取知识实体并保存 */
    @PostMapping("/extract/{articleId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> extract(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        return ResponseEntity.ok(ApiResponse.success(graphService.extractAndSave(articleId, content)));
    }

    /** 获取知识图谱数据，支持限制节点数量 */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraphData(
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getGraphData(limit)));
    }

    /** 搜索节点 */
    @GetMapping("/nodes/search")
    public ResponseEntity<ApiResponse<List<KnowledgeNode>>> searchNodes(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(graphService.searchNodes(keyword)));
    }

    /** 获取节点详情（包含关联节点和边） */
    @GetMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNodeDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(graphService.getNodeDetail(id)));
    }

    /** 获取按类型分组的节点 */
    @GetMapping("/nodes/by-type")
    public ResponseEntity<ApiResponse<Map<String, List<KnowledgeNode>>>> getNodesByType() {
        return ResponseEntity.ok(ApiResponse.success(graphService.getNodesByType()));
    }

    /** 删除节点（同时删除相关边） */
    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(@PathVariable Long id) {
        graphService.deleteNode(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 删除边 */
    @DeleteMapping("/edges/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEdge(@PathVariable Long id) {
        graphService.deleteEdge(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 手动添加节点 */
    @PostMapping("/nodes")
    public ResponseEntity<ApiResponse<KnowledgeNode>> addNode(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String type = body.getOrDefault("type", "concept");
        return ResponseEntity.ok(ApiResponse.success(graphService.addNode(name, type)));
    }

    /** 手动添加边 */
    @PostMapping("/edges")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addEdge(@RequestBody Map<String, Object> body) {
        Long sourceId = Long.valueOf(body.get("sourceId").toString());
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String relation = body.getOrDefault("relation", "关联").toString();
        return ResponseEntity.ok(ApiResponse.success(graphService.addEdge(sourceId, targetId, relation)));
    }

    /** 获取统计信息 */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(graphService.getStats()));
    }
}
