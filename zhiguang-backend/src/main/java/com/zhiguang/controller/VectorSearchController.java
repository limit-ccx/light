package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import com.zhiguang.dto.VectorSearchDto;
import com.zhiguang.service.AiService;
import com.zhiguang.service.VectorRebuildService;
import com.zhiguang.service.VectorSearchService;
import org.springframework.web.context.request.async.DeferredResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量检索接口
 */
@RestController
@RequestMapping("/api/vectors")
@RequiredArgsConstructor
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;
    private final AiService aiService;
    private final VectorRebuildService vectorRebuildService;

    /**
     * 文本相似性搜索
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VectorSearchDto>>> searchByText(
            @RequestParam String query,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int topK) {

        List<VectorSearchDto> results = vectorSearchService.searchByText(
                type, query, topK, aiService::generateEmbedding);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 向量相似性搜索（需要传入向量）
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<VectorSearchDto>>> searchByVector(
            @RequestBody VectorSearchRequest request) {

        List<VectorSearchDto> results = vectorSearchService.search(
                request.getType(), request.getVector(), request.getTopK());
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * 获取向量总数（管理员）
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> count(@RequestParam(required = false) String type) {
        long count = vectorSearchService.count(type);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 删除某类型所有向量（管理员）
     */
    @DeleteMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteByType(@PathVariable String type) {
        vectorSearchService.deleteVectorsByType(type);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 重建向量表（管理员）：DROP 后按当前 vector.postgres.dimension 重建。
     * 换 embedding 模型/维度后：先调此接口，再调 /admin/rebuild 全量重嵌入。
     */
    @PostMapping("/admin/recreate-table")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> recreateTable() {
        int dimension = vectorRebuildService.recreateTable();
        return ResponseEntity.ok(ApiResponse.success("向量表已重建，维度 " + dimension, dimension));
    }

    /**
     * 全量重嵌入（管理员）：分页遍历已发布文章重新生成向量。
     * 异步执行（每篇文章一次 embedding 调用），进度用 GET /api/vectors/count 观察。
     */
    @PostMapping("/admin/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public DeferredResult<ResponseEntity<ApiResponse<String>>> rebuild() {
        DeferredResult<ResponseEntity<ApiResponse<String>>> deferred = new DeferredResult<>(10_000L);
        vectorRebuildService.rebuildAllAsync();
        deferred.setResult(ResponseEntity.ok(ApiResponse.success(
                "全量重嵌入已开始（异步执行，可通过 /api/vectors/count 查看进度）", "STARTED")));
        return deferred;
    }

    public static class VectorSearchRequest {
        private String type;
        private List<Double> vector;
        private int topK = 10;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<Double> getVector() { return vector; }
        public void setVector(List<Double> vector) { this.vector = vector; }
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
    }
}
