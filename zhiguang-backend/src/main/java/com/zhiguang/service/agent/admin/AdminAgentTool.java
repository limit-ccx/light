package com.zhiguang.service.agent.admin;

import java.util.Map;

/**
 * 管理员智能体工具接口。
 * <p>
 * 与 C 端 AgentTool 分离（独立接口与注册表），避免 Spring 注入把管理侧工具混入 C 端白名单。
 * 写操作（write=true）在编排层被拦截转为待确认 draft（§5），confirm 后才调用 {@link #executeWrite}。
 */
public interface AdminAgentTool {

    String name();

    String description();

    /** OpenAI tools 协议的 parameters 对象 */
    Map<String, Object> parametersSchema();

    /** 是否写操作（true = 产出待确认卡片，不直接执行） */
    default boolean write() {
        return false;
    }

    /**
     * 写工具所属的权限模块（AdminPermissions）：null=通用。
     * draft 生成与 confirm 执行时按管理员职位校验（跨模块 agent 的权限边界，§5.5）。
     */
    default String module() {
        return null;
    }

    /** 风险级别：NORMAL（一键确认）/ STRICT（强确认，M3 预留） */
    default String riskLevel() {
        return "NORMAL";
    }

    /**
     * 执行（查询类工具由编排层直接调用）。
     * 写操作工具的本方法只会被编排层用于生成 draft 校验（参数合法性），
     * 真实执行走 {@link #executeWrite}。
     */
    String execute(Long adminId, Map<String, Object> args) throws Exception;

    /**
     * 真实执行写操作（管理员确认后由 ActionService 调用，仅 write=true 工具实现）。
     *
     * @return 执行结果描述（回注会话与前端）
     */
    default String executeWrite(Long adminId, Map<String, Object> args) throws Exception {
        throw new UnsupportedOperationException("not a write tool");
    }

    /**
     * 人话摘要（确认卡片展示），如"拒绝文章《标题》"。
     * 默认实现用工具名+参数，建议各写工具覆盖。
     */
    default String targetSummary(Map<String, Object> args) {
        return name() + " " + args;
    }
}
