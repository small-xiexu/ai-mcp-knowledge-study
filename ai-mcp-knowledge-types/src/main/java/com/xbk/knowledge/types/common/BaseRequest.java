package com.xbk.knowledge.types.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用请求基类
 * 所有 API 请求都应继承此类
 *
 * 职责：通用基础结构，用于统一分页与响应结构
 * @author sxie
 */
@Data
public class BaseRequest implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 链路追踪 ID
     * 用于日志关联和问题排查
     */
    private String traceId;

    /**
     * 请求时间戳
     * 客户端发起请求的时间
     */
    private Long timestamp;

    /**
     * 客户端版本号
     * 用于版本兼容性处理
     */
    private String clientVersion;
}
