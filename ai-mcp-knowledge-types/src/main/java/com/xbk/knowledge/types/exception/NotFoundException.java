package com.xbk.knowledge.types.exception;

import com.xbk.knowledge.types.common.ResultCode;

/**
 * 资源未找到异常
 * 用于表示请求的资源不存在
 *
 * 职责：通用异常定义，用于统一错误语义
 * @author sxie
 */
public class NotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public NotFoundException(String message) {
        super(ResultCode.NOT_FOUND.getCode(), message);
    }

    /**
     * 构造函数（带数据）
     *
     * @param message 错误消息
     * @param data    错误数据
     */
    public NotFoundException(String message, Object data) {
        super(ResultCode.NOT_FOUND.getCode(), message, data);
    }
}
