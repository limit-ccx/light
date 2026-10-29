package com.zhiguang.service.agent;

import java.util.Map;

/**
 * Agent 只读工具接口。
 * <p>
 * 约束（设计文档 §4.3）：
 * - 白名单制：仅注册进 AgentToolRegistry 的工具可被调用；
 * - 一律以服务端 userId 取数，绝不接受 LLM 传入的用户标识（防越权）；
 * - 返回 JSON 字符串，内容不含页面路径（agent 不做导航）。
 */
public interface AgentTool {

    /** 工具名（LLM 调用标识，snake_case） */
    String name();

    /** 工具描述（供模型决策是否调用） */
    String description();

    /** 入参 JSON Schema（OpenAI tools 格式的 parameters 对象） */
    Map<String, Object> parametersSchema();

    /**
     * 执行工具。
     *
     * @param userId 当前登录用户（服务端取，不信任 LLM 参数）
     * @param args   LLM 生成的入参（已解析为 Map）
     * @return JSON 字符串结果
     */
    String execute(Long userId, Map<String, Object> args) throws Exception;
}
