package com.xbk.knowledge.trigger.exception;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有 Controller 层抛出的异常
 *
 * 职责：接口层异常处理，用于统一错误返回
 * @author xiexu
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e       业务异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: path={}, code={}, message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage(), e.getData());
    }

    /**
     * 处理资源未找到异常
     *
     * @param e       资源未找到异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Object> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        log.warn("资源未找到: path={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(404, e.getMessage(), e.getData());
    }

    /**
     * 处理参数校验异常（@Valid 注解触发）
     *
     * @param e       参数校验异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("参数校验失败: path={}", request.getRequestURI());

        // 收集所有字段的错误信息
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return Result.error(400, "参数校验失败", errors);
    }

    /**
     * 处理绑定异常（表单提交时触发）
     *
     * @param e       绑定异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("参数绑定失败: path={}", request.getRequestURI());

        // 收集所有字段的错误信息
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return Result.error(400, "参数绑定失败", errors);
    }

    /**
     * 处理非法参数异常
     *
     * @param e       非法参数异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: path={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理运行时异常
     *
     * @param e       运行时异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常: path={}, message={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(500, "系统内部错误：" + e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param e       异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常: path={}, message={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(500, "系统内部错误");
    }
}
