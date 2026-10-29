package com.zhiguang.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台职位与模块权限矩阵。
 * 通用模块（看板统计、数据统计、反馈、操作日志）对所有职位开放，
 * 其余模块按职位隔离；超级管理员拥有全部权限。
 */
public final class AdminPermissions {

    public static final String SUPER = "SUPER";
    public static final String OPERATION = "OPERATION";
    public static final String CONTENT = "CONTENT";
    public static final String COMMUNITY = "COMMUNITY";
    public static final String USER = "USER";
    public static final String FEEDBACK = "FEEDBACK";
    public static final String SYSTEM = "SYSTEM";
    public static final String DATA = "DATA";

    public static final List<String> ALL_POSITIONS =
            List.of(SUPER, OPERATION, CONTENT, COMMUNITY, USER, FEEDBACK, SYSTEM, DATA);

    /** 模块 → 允许的职位（未列出的模块视为通用模块） */
    private static final Map<String, Set<String>> MODULE_POSITIONS = Map.of(
            "CONTENT", Set.of(SUPER, CONTENT),
            "COMMUNITY", Set.of(SUPER, COMMUNITY),
            "USER", Set.of(SUPER, USER),
            "OPERATIONS", Set.of(SUPER, OPERATION),
            "REMINDERS", Set.of(SUPER, OPERATION),
            "SYSTEM", Set.of(SUPER, SYSTEM)
    );

    private AdminPermissions() {
    }

    public static boolean isPosition(String position) {
        return position != null && ALL_POSITIONS.contains(position);
    }

    public static boolean can(String position, String module) {
        if (position == null) {
            return false;
        }
        if (SUPER.equals(position)) {
            return true;
        }
        Set<String> allowed = MODULE_POSITIONS.get(module);
        return allowed != null && allowed.contains(position);
    }

    public static boolean isSuper(String position) {
        return SUPER.equals(position);
    }

    /** 是否具备管理员管理权限（分配职位） */
    public static boolean canManageAdmins(String position) {
        return SUPER.equals(position) || OPERATION.equals(position);
    }

    /** 是否是「运营管理」这一受限职位 */
    public static boolean isAdminManagerOnly(String position) {
        return OPERATION.equals(position);
    }

    public static List<String> allPositions() {
        return ALL_POSITIONS;
    }
}
