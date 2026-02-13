package com.xbk.knowledge.trigger.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
     * 处理 Sa-Token 未登录异常
     *
     * 为什么：前端需要稳定识别 401 语义并触发重新登录。
     *
     * @param e       未登录异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Object> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String message = e.getMessage();
        log.warn("未登录访问: path={}, message={}", requestUri, message);
        return Result.error(ResultCode.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getMessage());
    }

    /**
     * 处理 Sa-Token 无权限异常
     *
     * 为什么：明确区分 403，便于前端做权限提示。
     *
     * @param e       无权限异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Object> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String permission = e.getPermission();
        log.warn("权限不足: path={}, permission={}", requestUri, permission);
        return Result.error(ResultCode.FORBIDDEN, ResultCode.FORBIDDEN.getMessage());
    }

    /**
     * 处理业务异常
     *
     * 为什么：业务异常需要按业务码返回，避免被统一为 500。
     *
     * @param e       业务异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        Integer code = e.getCode();
        String message = e.getMessage();
        Object data = e.getData();
        log.warn("业务异常: path={}, code={}, message={}", requestUri, code, message);
        return Result.error(code, message, data);
    }

    /**
     * 处理资源未找到异常
     *
     * 为什么：区分 404 语义，便于前端与监控系统识别。
     *
     * @param e       资源未找到异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Object> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String message = e.getMessage();
        Object data = e.getData();
        log.warn("资源未找到: path={}, message={}", requestUri, message);
        return Result.error(ResultCode.NOT_FOUND, message, data);
    }

    /**
     * 处理参数校验异常（@Valid 注解触发）
     *
     * 为什么：集中收敛校验错误，返回字段级错误信息便于前端提示。
     *
     * @param e       参数校验异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.warn("参数校验失败: path={}", requestUri);

        // 收集所有字段的错误信息
        Map<String, String> errors = new HashMap<>();
        Consumer<ObjectError> errorConsumer = error -> {
            FieldError fieldError = (FieldError) error;
            String fieldName = fieldError.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        };
        e
                .getBindingResult()
                .getAllErrors()
                .forEach(errorConsumer);

        return Result.error(ResultCode.PARAM_VALIDATION_FAILED, errors);
    }

    /**
     * 处理绑定异常（表单提交时触发）
     *
     * 为什么：表单绑定失败需给出具体字段错误，避免泛化为 500。
     *
     * @param e       绑定异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.warn("参数绑定失败: path={}", requestUri);

        // 收集所有字段的错误信息
        Map<String, String> errors = new HashMap<>();
        Consumer<ObjectError> errorConsumer = error -> {
            FieldError fieldError = (FieldError) error;
            String fieldName = fieldError.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        };
        e
                .getBindingResult()
                .getAllErrors()
                .forEach(errorConsumer);

        return Result.error(ResultCode.PARAM_BIND_FAILED, errors);
    }

    /**
     * 处理非法参数异常
     *
     * 为什么：参数非法属于客户端问题，应返回 400 便于纠错。
     *
     * @param e       非法参数异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String message = e.getMessage();
        log.warn("非法参数: path={}, message={}", requestUri, message);
        return Result.error(ResultCode.BAD_REQUEST, message);
    }

    /**
     * 处理运行时异常
     *
     * 为什么：运行时异常统一兜底，确保接口返回结构稳定。
     *
     * @param e       运行时异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String message = e.getMessage();
        String responseMessage = "系统内部错误：" + message;
        log.error("运行时异常: path={}, message={}", requestUri, message, e);
        return Result.error(ResultCode.INTERNAL_ERROR, responseMessage);
    }

    /**
     * 处理所有未捕获的异常
     *
     * 为什么：最后一道兜底，防止异常逃逸导致非 JSON 响应。
     *
     * @param e       异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String message = e.getMessage();
        log.error("未知异常: path={}, message={}", requestUri, message, e);
        return Result.error(ResultCode.INTERNAL_ERROR);
    }
}
