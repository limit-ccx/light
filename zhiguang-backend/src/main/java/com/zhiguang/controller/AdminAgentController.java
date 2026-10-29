package com.zhiguang.controller;

import com.zhiguang.config.AdminAgentConfig;
import com.zhiguang.dto.ApiResponse;
import com.zhiguang.entity.AdminKbDoc;
import com.zhiguang.entity.User;
import com.zhiguang.repository.AdminKbDocRepository;
import com.zhiguang.service.agent.admin.AdminAgentActionService;
import com.zhiguang.service.agent.admin.AdminAgentChatService;
import com.zhiguang.service.agent.admin.AdminAgentSessionService;
import com.zhiguang.service.agent.admin.AdminKbRagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 管理员智能体（运营助手）接口。
 * <p>
 * 权限：/api/admin/** 由 SecurityConfig 强制 hasRole(ADMIN)，方法级 @PreAuthorize 纵深防御；
 * confirm 执行前 ActionService 再校验 operationId 归属当前管理员（§5.5）。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgentController {

    private final AdminAgentChatService chatService;
    private final com.zhiguang.security.AgentAbortRegistry abortRegistry;
    private final AdminAgentSessionService sessionService;
    private final AdminAgentActionService actionService;
    private final AdminAgentConfig config;
    private final AdminKbDocRepository kbDocRepository;
    private final AdminKbRagService ragService;

    /** SSE 对话（事件协议见 AdminAgentChatService） */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, String> body,
                           @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return emitterError("UNAUTHORIZED", "请先登录");
        }
        String message = body == null ? "" : body.getOrDefault("message", "").trim();
        String provider = body == null ? null : body.get("provider");
        String effort = body == null ? null : body.get("effort");
        if (message.isEmpty()) {
            return emitterError("EMPTY", "消息不能为空");
        }
        if (message.length() > 800) {
            return emitterError("TOO_LONG", "单条消息不能超过 800 字");
        }
        if (!config.isEnabled()) {
            return emitterError("DISABLED", "运营助手暂未开放");
        }
        if (!chatService.tryConsumeQuota(admin.getId())) {
            return emitterError("QUOTA_EXCEEDED", "今日助手配额已用完（" + config.getDailyQuota() + " 次/天）");
        }

        SseEmitter emitter = new SseEmitter(config.getRequestTimeoutSeconds() * 1000L + 10_000L);
        chatService.streamChat(admin, message, provider, effort, emitter);
        return emitter;
    }

    /** 可用模型平台与密钥配置状态（前端下拉数据源；不返回密钥本身） */
    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> models() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        chatService.availableProviders().forEach((name, setting) -> result.add(Map.of(
                "provider", name,
                "model", setting.getModel(),
                "reasoningModel", setting.getReasoningModel() == null ? "" : setting.getReasoningModel(),
                "ready", setting.getApiKey() != null && !setting.getApiKey().isBlank()
                        && !setting.getApiKey().startsWith("${")
                        && setting.getBaseUrl() != null && !setting.getBaseUrl().isBlank()
                        && !setting.getBaseUrl().startsWith("${"))));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 持久化对话历史（分页，跨会话回看） */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 100);
        var result = chatService.getChatHistory(admin.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "list", result.getContent(),
                "total", result.getTotalElements(),
                "page", page)));
    }

    /**
     * 停止当前生成（前端"停止"按钮）。
     */
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<Boolean>> stop(@AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success("已发送停止指令",
                abortRegistry.abort(admin.getId())));
    }

    /**
     * 插入对话：生成进行中时追加新问题——落会话 note，当前轮结束后引擎按插入顺序接续处理。
     */
    @PostMapping("/insert")
    public ResponseEntity<ApiResponse<Void>> insert(@RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        String question = body == null ? "" : body.getOrDefault("message", "").trim();
        if (question.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "问题不能为空"));
        }
        sessionService.addNote(admin.getId(), "【插入的问题】" + question);
        return ResponseEntity.ok(ApiResponse.success("已插入，本轮结束后处理", null));
    }

    /** 确认/拒绝待确认操作 */
    @PostMapping("/actions/{operationId}/confirm")
    public ResponseEntity<ApiResponse<String>> confirmAction(@PathVariable String operationId,
                                                             @RequestBody Map<String, Object> body,
                                                             @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        boolean approve = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approve", "false")));
        String result = actionService.confirm(admin.getId(), operationId, approve);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 当前待确认操作列表 */
    @GetMapping("/actions/pending")
    public ResponseEntity<ApiResponse<List<AdminAgentActionService.PendingAction>>> pendingActions(
            @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success(actionService.listPending(admin.getId())));
    }

    /** 会话恢复 */
    @GetMapping("/conversations/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> currentConversation(
            @AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        AdminAgentSessionService.Session session = sessionService.load(admin.getId());
        if (session == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("conversationId", "", "messages", List.of())));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "conversationId", session.getConversationId(),
                "messages", session.getMessages())));
    }

    /** 清空会话 */
    @DeleteMapping("/conversations/current")
    public ResponseEntity<ApiResponse<Void>> clearConversation(@AuthenticationPrincipal User admin) {
        if (admin == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录或登录已过期"));
        }
        sessionService.clear(admin.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 知识文档 CRUD（保存即异步重建索引） */
    @GetMapping("/kb/docs")
    public ResponseEntity<ApiResponse<List<AdminKbDoc>>> listDocs() {
        return ResponseEntity.ok(ApiResponse.success(kbDocRepository.findAll()));
    }

    @PostMapping("/kb/docs")
    public ResponseEntity<ApiResponse<AdminKbDoc>> createDoc(@Valid @RequestBody AdminKbDoc doc,
                                                             @AuthenticationPrincipal User admin) {
        doc.setId(null);
        doc.setStatus("DRAFT");
        doc.setCreatedBy(admin == null ? null : admin.getId());
        AdminKbDoc saved = kbDocRepository.save(doc);
        ragService.reindexAsync(saved.getId());
        return ResponseEntity.ok(ApiResponse.success("已保存并开始索引", saved));
    }

    @PutMapping("/kb/docs/{id}")
    public ResponseEntity<ApiResponse<AdminKbDoc>> updateDoc(@PathVariable Long id,
                                                             @Valid @RequestBody AdminKbDoc doc,
                                                             @AuthenticationPrincipal User admin) {
        AdminKbDoc existing = kbDocRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "文档不存在"));
        }
        existing.setTitle(doc.getTitle());
        existing.setSourceType(doc.getSourceType());
        existing.setContent(doc.getContent());
        existing = kbDocRepository.save(existing);
        ragService.reindexAsync(existing.getId());
        return ResponseEntity.ok(ApiResponse.success("已更新并开始重建索引", existing));
    }

    @DeleteMapping("/kb/docs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoc(@PathVariable Long id) {
        kbDocRepository.deleteById(id);
        ragService.reindexAsync(-1L); // no-op 保护：删除文档的块由 reindex 内部处理
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 重建索引（FAILED 重试 / 手动刷新） */
    @PostMapping("/kb/docs/{id}/reindex")
    public ResponseEntity<ApiResponse<String>> reindexDoc(@PathVariable Long id) {
        int chunks = ragService.reindex(id);
        return ResponseEntity.ok(ApiResponse.success("已重建索引：" + chunks + " 个分块", null));
    }

    private SseEmitter emitterError(String code, String message) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("code", code, "message", message)));
            emitter.complete();
        } catch (Exception ignore) {
        }
        return emitter;
    }
}
