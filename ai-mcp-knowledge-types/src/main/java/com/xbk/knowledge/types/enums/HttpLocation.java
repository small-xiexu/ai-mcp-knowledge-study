package com.xbk.knowledge.types.enums;

/**
 * HTTP 参数位置枚举
 *
 * 职责：定义参数在 HTTP 请求中的位置
 * @author sxie
 */
public enum HttpLocation {
    /**
     * 请求体
     */
     BODY,
    /**
     * URL 查询参数
     */
     QUERY,
    /**
     * URL 路径参数
     */
     PATH,
    /**
     * 请求头
     */
     HEADER
}
