package com.xbk.knowledge.types.exception;

/**
 * 内部不变量异常
 * 用于表示系统内部状态与预期不一致的情况
 *
 * 职责：通用异常定义，用于表达内部流程不变量破坏
 *
 * @author sxie
 */
public class InternalInvariantException extends RuntimeException {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数（仅消息）。
     *
     * @param message 错误消息
     */
    public InternalInvariantException(String message) {
        super(message);
    }

    /**
     * 构造函数（消息 + 原因）。
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public InternalInvariantException(String message, Throwable cause) {
        super(message, cause);
    }
}
