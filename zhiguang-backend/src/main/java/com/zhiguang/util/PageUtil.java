package com.zhiguang.util;

/**
 * 分页工具类：强制限制页大小，防止恶意请求全表扫描
 */
public class PageUtil {

    /** 通用列表最大页大小 */
    public static final int MAX_PAGE_SIZE = 50;

    /** 粉丝/关注列表最大页大小 */
    public static final int MAX_FOLLOW_PAGE_SIZE = 100;

    private PageUtil() {
    }

    /**
     * 限制页大小不超过通用上限（50），且不小于 1
     */
    public static int clampSize(int size) {
        return clampSize(size, MAX_PAGE_SIZE);
    }

    /**
     * 限制页大小不超过指定上限，且不小于 1
     */
    public static int clampSize(int size, int maxSize) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, maxSize);
    }

    /**
     * 限制页码不小于 0
     */
    public static int clampPage(int page) {
        return Math.max(page, 0);
    }
}
