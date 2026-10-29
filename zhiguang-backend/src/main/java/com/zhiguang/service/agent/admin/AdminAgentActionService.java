package com.zhiguang.service.agent.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiguang.config.AdminAgentConfig;
import com.zhiguang.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 写操作安全链路（设计方案 §5）：draft → confirm → 执行 → 审计 → 回注。
 * <p>
 * - draft：Redis 挂起（TTL 10min），同对象同动作指纹去重（幂等，L8）；
 * - confirm：归属校验 + 状态校验（一次执行/一次拒绝）+ ADMIN 再校验由 Controller 层保障；
 * - 审计：AGENT_DRAFT / AGENT_EXEC / AGENT_CANCEL 写操作日志；
 * - 回注：执行结果作为系统通知进会话（下一轮模型可见）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAgentActionService {

    private static final String PENDING_PREFIX = "admin-agent:pending:";
    private static final String PENDING_SET_PREFIX = "admin-agent:pending-set:";

    private final StringRedisTemplate redisTemplate;
    private final com.zhiguang.repository.UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminToolRegistry registry;
    private final AdminAgentSessionService sessionService;
    private final AdminLogService adminLogService;
    private final AdminAgentConfig config;

    public static class PendingAction {
        private String operationId;
        private Long adminId;
        private String tool;
        private Map<String, Object> args = new HashMap<>();
        private String riskLevel = "NORMAL";
        private String reason = "";
        private String targetSummary = "";
        private String status = "PENDING"; // PENDING / EXECUTED / REJECTED
        private long createdAt = System.currentTimeMillis();

        public String getOperationId() { return operationId; }
        public void setOperationId(String operationId) { this.operationId = operationId; }
        public Long getAdminId() { return adminId; }
        public void setAdminId(Long adminId) { this.adminId = adminId; }
        public String getTool() { return tool; }
        public void setTool(String tool) { this.tool = tool; }
        public Map<String, Object> getArgs() { return args; }
        public void setArgs(Map<String, Object> args) { this.args = args; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getTargetSummary() { return targetSummary; }
        public void setTargetSummary(String targetSummary) { this.targetSummary = targetSummary; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }

    /**
     * 生成待确认操作（幂等：同指纹返回已有 operationId）。
     */
    public PendingAction createDraft(Long adminId, String toolName, Map<String, Object> args,
                                     String reason) {
        AdminAgentTool tool = registry.get(toolName);
        if (tool == null || !tool.write()) {
            throw new IllegalArgumentException("非写操作工具: " + toolName);
        }
        checkModulePermission(adminId, tool);

        // 指纹去重：同工具同关键参数的 pending draft 复用
        for (PendingAction existing : listPending(adminId)) {
            if (toolName.equals(existing.getTool()) && fingerprint(args).equals(fingerprint(existing.getArgs()))) {
                return existing;
            }
        }

        PendingAction action = new PendingAction();
        action.setOperationId(UUID.randomUUID().toString().replace("-", ""));
        action.setAdminId(adminId);
        action.setTool(toolName);
        action.setArgs(args);
        action.setRiskLevel(tool.riskLevel());
        action.setReason(reason == null ? "" : reason);
        action.setTargetSummary(tool.targetSummary(args));

        try {
            redisTemplate.opsForValue().set(PENDING_PREFIX + action.getOperationId(),
                    objectMapper.writeValueAsString(action),
                    Duration.ofMinutes(config.getPendingTtlMinutes()));
            redisTemplate.opsForSet().add(PENDING_SET_PREFIX + adminId, action.getOperationId());
            redisTemplate.expire(PENDING_SET_PREFIX + adminId, Duration.ofHours(24));
        } catch (Exception e) {
            throw new IllegalStateException("draft 持久化失败", e);
        }

        adminLogService.log(adminId, "AGENT_DRAFT", "AGENT", null,
                "draft: " + action.getTargetSummary() + " (tool=" + toolName + ", id=" + action.getOperationId() + ")");
        log.info("Admin Agent draft: adminId={}, tool={}, opId={}", adminId, toolName, action.getOperationId());
        return action;
    }

    /**
     * 确认/拒绝执行。
     *
     * @return 结果描述（前端展示 + 回注会话）
     */
    public String confirm(Long adminId, String operationId, boolean approve) {
        PendingAction action = load(adminId, operationId);
        if (action == null) {
            return "操作不存在或已过期";
        }
        if (!"PENDING".equals(action.getStatus())) {
            return "该操作已" + ("EXECUTED".equals(action.getStatus()) ? "执行" : "取消") + "，无需重复处理";
        }

        if (!approve) {
            action.setStatus("REJECTED");
            save(action);
            adminLogService.log(adminId, "AGENT_CANCEL", "AGENT", null,
                    "取消: " + action.getTargetSummary() + " (id=" + operationId + ")");
            String note = "❌ 已取消：" + action.getTargetSummary();
            sessionService.addNote(adminId, note);
            return note;
        }

        AdminAgentTool tool = registry.get(action.getTool());
        checkModulePermission(adminId, tool); // confirm 时再校验（防期间降职，§5.5）
        try {
            String result = tool.executeWrite(adminId, action.getArgs());
            action.setStatus("EXECUTED");
            save(action);
            adminLogService.log(adminId, "AGENT_EXEC", "AGENT", null,
                    "执行: " + action.getTargetSummary() + " → " + result + " (id=" + operationId + ")");
            String note = "✅ 已执行：" + action.getTargetSummary() + "（" + result + "）";
            sessionService.addNote(adminId, note);
            log.info("Admin Agent exec: adminId={}, op={}, result={}", adminId, operationId, result);
            return note;
        } catch (Exception e) {
            log.error("Admin Agent 执行失败: op={}", operationId, e);
            // 失败保持 PENDING 可重试？不：标记执行失败也不重试，返回错误说明
            action.setStatus("REJECTED");
            save(action);
            adminLogService.log(adminId, "AGENT_EXEC", "AGENT", null,
                    "执行失败: " + action.getTargetSummary() + " err=" + e.getMessage());
            return "⚠️ 执行失败：" + action.getTargetSummary() + "（" + e.getMessage() + "）";
        }
    }

    public List<PendingAction> listPending(Long adminId) {
        List<PendingAction> result = new ArrayList<>();
        try {
            var members = redisTemplate.opsForSet().members(PENDING_SET_PREFIX + adminId);
            if (members == null) {
                return result;
            }
            for (String id : members) {
                PendingAction action = load(adminId, id);
                if (action != null && "PENDING".equals(action.getStatus())) {
                    result.add(action);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    /** 按管理员职位校验工具所属模块权限（SUPER 全通过） */
    private void checkModulePermission(Long adminId, AdminAgentTool tool) {
        if (tool.module() == null) {
            return;
        }
        String position = userRepository.findById(adminId)
                .map(u -> u.getAdminPosition())
                .orElse(null);
        if (!com.zhiguang.security.AdminPermissions.can(position, tool.module())) {
            throw new IllegalArgumentException("当前职位无权执行该操作（模块 " + tool.module() + "），请在对应管理页操作");
        }
    }

    private PendingAction load(Long adminId, String operationId) {
        try {
            String json = redisTemplate.opsForValue().get(PENDING_PREFIX + operationId);
            if (json == null) {
                return null;
            }
            PendingAction action = objectMapper.readValue(json, PendingAction.class);
            if (!adminId.equals(action.getAdminId())) {
                return null; // 归属校验（§5.5）
            }
            return action;
        } catch (Exception e) {
            return null;
        }
    }

    private void save(PendingAction action) {
        try {
            Duration ttl = Duration.ofMinutes(config.getPendingTtlMinutes())
                    .plusMillis(action.getCreatedAt() - System.currentTimeMillis() + Duration.ofMinutes(10).toMillis());
            redisTemplate.opsForValue().set(PENDING_PREFIX + action.getOperationId(),
                    objectMapper.writeValueAsString(action), Duration.ofMinutes(30));
        } catch (Exception ignored) {
        }
    }

    private String fingerprint(Map<String, Object> args) {
        try {
            Map<String, Object> sorted = new java.util.TreeMap<>(args);
            return objectMapper.writeValueAsString(sorted);
        } catch (Exception e) {
            return String.valueOf(args);
        }
    }
}
