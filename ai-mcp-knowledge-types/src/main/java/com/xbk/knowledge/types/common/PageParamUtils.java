package com.xbk.knowledge.types.common;

/**
 * 分页参数工具类。
 *
 * 职责：统一 offset/pageNum/pageSize 的归一化与转换逻辑。
 *
 * @author sxie
 */
public final class PageParamUtils {

    /**
     * 默认每页条数。
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    private PageParamUtils() {
    }

    /**
     * 归一化偏移量。
     *
     * @param offset 原始偏移量
     * @return 归一化后的偏移量（最小为 0）
     */
    public static int normalizeOffset(Integer offset) {
        if (offset == null || offset < 0) {
            return 0;
        }
        return offset;
    }

    /**
     * 归一化页码。
     *
     * @param pageNum 原始页码
     * @return 归一化后的页码（最小为 1）
     */
    public static int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return 1;
        }
        return pageNum;
    }

    /**
     * 归一化每页条数。
     *
     * @param pageSize 原始每页条数
     * @param defaultPageSize 默认每页条数（<=0 时回退为 10）
     * @return 归一化后的每页条数（最小为 1）
     */
    public static int normalizePageSize(Integer pageSize, int defaultPageSize) {
        return normalizePageSize(pageSize, defaultPageSize, Integer.MAX_VALUE);
    }

    /**
     * 归一化每页条数（可限制最大值）。
     *
     * @param pageSize 原始每页条数
     * @param defaultPageSize 默认每页条数（<=0 时回退为 10）
     * @param maxPageSize 最大每页条数（<=0 时不限制）
     * @return 归一化后的每页条数
     */
    public static int normalizePageSize(Integer pageSize, int defaultPageSize, int maxPageSize) {
        int fallback = defaultPageSize > 0 ? defaultPageSize : DEFAULT_PAGE_SIZE;
        int safe = pageSize == null || pageSize < 1 ? fallback : pageSize;
        if (maxPageSize > 0) {
            return Math.min(safe, maxPageSize);
        }
        return safe;
    }

    /**
     * 将 offset + pageSize 转为页码。
     *
     * @param offset 偏移量
     * @param pageSize 每页条数
     * @return 页码（从 1 开始）
     */
    public static int offsetToPageNum(int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safePageSize = Math.max(pageSize, 1);
        return safeOffset / safePageSize + 1;
    }

    /**
     * 将页码转为 offset。
     *
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 偏移量
     */
    public static int pageNumToOffset(int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = Math.max(pageSize, 1);
        return (safePageNum - 1) * safePageSize;
    }
}
