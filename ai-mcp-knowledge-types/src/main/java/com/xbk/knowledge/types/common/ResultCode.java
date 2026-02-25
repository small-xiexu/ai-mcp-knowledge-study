package com.xbk.knowledge.types.common;

import lombok.Getter;

/**
 * 统一响应码枚举
 * 用于集中管理响应码与默认消息，避免散落在业务代码中
 *
 * 职责：统一错误语义，降低维护成本
 * @author sxie
 */
@Getter
public enum ResultCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 通用参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 参数校验失败
     */
    PARAM_VALIDATION_FAILED(400, "参数校验失败"),

    /**
     * 参数绑定失败
     */
    PARAM_BIND_FAILED(400, "参数绑定失败"),

    /**
     * 未登录
     */
    UNAUTHORIZED(401, "未登录或登录已失效"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源未找到
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 系统内部错误
     */
    INTERNAL_ERROR(500, "系统内部错误"),

    /**
     * AI 调用失败
     */
    AI_CALL_FAILED(500, "AI 调用失败"),

    /**
     * 模型选择策略未实现
     */
    STRATEGY_NOT_SUPPORTED(400, "模型选择策略未实现");

    /**
     * 业务码。
     */
    private final Integer code;

    /**
     * 默认消息。
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
