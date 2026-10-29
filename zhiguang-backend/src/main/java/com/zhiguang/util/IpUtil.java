package com.zhiguang.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 获取客户端真实 IP 工具
 * <p>
 * X-Forwarded-For 格式为 "客户端, 代理1, 代理2..."，其中除最后一跳由最近的
 * 可信反代追加外，其余段落均可被客户端伪造。因此多级代理时取<b>末段</b>（最近
 * 可信代理看到的地址），而不是首段（攻击者可控）。
 */
public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理：取末段（由最近的受信反代写入），首段攻击者可控
        if (ip != null && ip.contains(",")) {
            String[] hops = ip.split(",");
            ip = hops[hops.length - 1].trim();
        }
        return (ip == null || ip.isEmpty()) ? "unknown" : ip;
    }
}
