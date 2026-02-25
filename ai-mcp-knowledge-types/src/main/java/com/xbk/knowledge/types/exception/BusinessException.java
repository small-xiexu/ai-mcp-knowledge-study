package com.xbk.knowledge.types.exception;

import com.xbk.knowledge.types.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑中的异常情况
 *
 * 职责：通用异常定义，用于统一错误语义
 * @author sxie
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误数据
     */
    private final Object data;

    /**
     * 构造函数（仅消息）
     * 
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this
                .code = ResultCode
                .BAD_REQUEST
                .getCode();
        this.data = null;
    }

    /**
     * 构造函数（错误码 + 消息）
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    /**
     * 构造函数（错误码 + 消息 + 数据）
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param data 错误数据
     */
    public BusinessException(Integer code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    /**
     * 构造函数（消息 + 原因）
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this
                .code = ResultCode
                .BAD_REQUEST
                .getCode();
        this.data = null;
    }
}
