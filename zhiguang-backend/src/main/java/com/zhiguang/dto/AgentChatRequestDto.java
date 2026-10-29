package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 对话请求。
 * pageContext 由前端携带（当前路由/模块），Phase 1 仅作日志参考、不参与工具调用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRequestDto {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "单条消息不能超过 500 字")
    private String message;

    /** 续接会话时回传 done 事件中的 conversationId */
    private String conversationId;

    /** 当前页面上下文（可选） */
    private Map<String, Object> pageContext;
}
