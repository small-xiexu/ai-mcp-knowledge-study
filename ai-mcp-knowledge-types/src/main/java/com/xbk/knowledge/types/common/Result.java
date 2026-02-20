package com.xbk.knowledge.types.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 统一响应结果类
 * 用于封装所有 API 的响应数据
 *
 * 职责：通用基础结构，用于统一分页与响应结构
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码（200: 成功，其他: 失败）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应（无数据）
     *
     * @return Result
     */
    public static <T> Result<T> success() {
        Integer code = ResultCode.SUCCESS.getCode();
        String message = ResultCode.SUCCESS.getMessage();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        Integer code = ResultCode.SUCCESS.getCode();
        String message = ResultCode.SUCCESS.getMessage();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @return Result
     */
    public static <T> Result<T> success(String message, T data) {
        Integer code = ResultCode.SUCCESS.getCode();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @return Result
     */
    public static <T> Result<T> error(String message) {
        Integer code = ResultCode.INTERNAL_ERROR.getCode();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应（自定义错误码）
     *
     * @param code    错误码
     * @param message 错误消息
     * @return Result
     */
    public static <T> Result<T> error(Integer code, String message) {
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应（使用统一错误码）
     *
     * @param resultCode 错误码
     * @return Result
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        Integer code = resultCode.getCode();
        String message = resultCode.getMessage();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应（统一错误码 + 自定义消息）
     *
     * @param resultCode 错误码
     * @param message    错误消息
     * @return Result
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Integer code = resultCode.getCode();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应（统一错误码 + 数据）
     *
     * @param resultCode 错误码
     * @param data       错误数据
     * @return Result
     */
    public static <T> Result<T> error(ResultCode resultCode, T data) {
        Integer code = resultCode.getCode();
        String message = resultCode.getMessage();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应（统一错误码 + 自定义消息 + 数据）
     *
     * @param resultCode 错误码
     * @param message    错误消息
     * @param data       错误数据
     * @return Result
     */
    public static <T> Result<T> error(ResultCode resultCode, String message, T data) {
        Integer code = resultCode.getCode();
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();
    }

    /**
     * 失败响应（带数据）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    错误数据
     * @return Result
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        Long timestamp = System.currentTimeMillis();
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(timestamp)
                .build();
    }
}
