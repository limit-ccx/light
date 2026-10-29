package com.zhiguang.interceptor;

import com.zhiguang.entity.User;
import com.zhiguang.security.AdminPermissions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 后台模块级权限校验：按职位限制可访问的管理模块。
 * 只拦截 /api/admin/**，未登录与角色校验仍由 Spring Security 负责。
 */
@Component
public class AdminModuleInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PREFIX = "/api/admin/";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return true;
        }
        if (!"ADMIN".equals(user.getRole())) {
            return true;
        }

        String position = user.getAdminPosition();
        if (AdminPermissions.isSuper(position)) {
            return true;
        }

        String module = resolveModule(request.getRequestURI());
        if (module == null || AdminPermissions.can(position, module)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"code\":403,\"message\":\"当前职位无权访问该模块\",\"data\":null}"
        );
        return false;
    }

    /** 路径 → 模块；返回 null 表示通用模块（所有职位可访问） */
    private String resolveModule(String uri) {
        if (!uri.startsWith(ADMIN_PREFIX)) {
            return null;
        }
        String path = uri.substring(ADMIN_PREFIX.length());
        // 归一化：集合端点（如 /api/admin/users）不带尾斜杠，需补齐后再做前缀匹配
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        // 看板需要读取管理员概览，属于通用数据
        if (path.startsWith("users/overview")) {
            return null;
        }
        if (path.startsWith("content/") || path.startsWith("recommend/")) {
            return "CONTENT";
        }
        if (path.startsWith("operations/")) {
            return "OPERATIONS";
        }
        if (path.startsWith("reminders/")) {
            return "REMINDERS";
        }
        if (path.startsWith("tree-hole/")) {
            return "COMMUNITY";
        }
        if (path.startsWith("users/")) {
            return "USER";
        }
        if (path.startsWith("config/")) {
            return "SYSTEM";
        }
        // agent 是跨模块工具：路径放开为通用，权限边界下沉到工具执行层
        // （AdminAgentActionService 按职位校验各写工具所属模块）
        // statistics / feedback / logs 为通用模块（日志只读，用于看板动态与审计）
        return null;
    }
}
