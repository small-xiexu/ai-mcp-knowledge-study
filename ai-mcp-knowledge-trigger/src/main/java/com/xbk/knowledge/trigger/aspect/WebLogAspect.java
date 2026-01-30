package com.xbk.knowledge.trigger.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xbk.knowledge.types.time.TimeCostUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Web 请求日志切面
 * 统一记录所有 HTTP 接口的请求和响应信息
 *
 * 职责：接口层横切逻辑，用于统一日志与追踪
 * @author xiexu
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WebLogAspect {

    private final ObjectMapper objectMapper;

    /**
     * 定义切点：拦截 trigger.http 包下所有 Controller 的公共方法
     */
    @Pointcut("execution(public * com.xbk.knowledge.trigger.http..*Controller.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知：记录请求和响应信息
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = TimeCostUtils.start();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 获取请求信息
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String traceId = TraceIdUtils.getOrCreateTraceId();

        // 记录请求信息
        if (request != null) {
            logRequest(joinPoint, request, traceId);
        }

        // 执行目标方法
        Object result = null;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            // 记录响应信息
            long costTime = TimeCostUtils.costMillis(startTime);
            logResponse(joinPoint, result, exception, costTime, traceId);
        }
    }

    /**
     * 记录请求信息
     *
     * @param joinPoint 连接点
     * @param request   HTTP 请求
     * @param traceId   链路追踪 ID
     */
    private void logRequest(ProceedingJoinPoint joinPoint, HttpServletRequest request, String traceId) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        int lastDotIndex = className.lastIndexOf('.');
        int classNameStart = lastDotIndex + 1;
        String simpleClassName = className.substring(classNameStart);

        // 获取请求参数
        Object[] args = joinPoint.getArgs();
        String params = formatParams(args);
        String httpMethod = request.getMethod();
        String requestUri = request.getRequestURI();

        log.info("[{}] >>> 请求开始 | {} {} | {}.{} | 参数: {}",
                traceId,
                httpMethod,
                requestUri,
                simpleClassName,
                methodName,
                params);
    }

    /**
     * 记录响应信息
     *
     * @param joinPoint 连接点
     * @param result    返回结果
     * @param exception 异常信息
     * @param costTime  耗时（毫秒）
     * @param traceId   链路追踪 ID
     */
    private void logResponse(ProceedingJoinPoint joinPoint, Object result,
                              Throwable exception, long costTime, String traceId) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        int lastDotIndex = className.lastIndexOf('.');
        int classNameStart = lastDotIndex + 1;
        String simpleClassName = className.substring(classNameStart);

        if (exception != null) {
            // 记录异常
            String exceptionMessage = exception.getMessage();
            log.error("[{}] <<< 请求异常 | {}.{} | 耗时: {}ms | 异常: {}",
                    traceId,
                    simpleClassName,
                    methodName,
                    costTime,
                    exceptionMessage);
        } else {
            // 记录正常响应
            String response = formatResponse(result);
            log.info("[{}] <<< 请求结束 | {}.{} | 耗时: {}ms | 响应: {}",
                    traceId,
                    simpleClassName,
                    methodName,
                    costTime,
                    response);
        }
    }

    /**
     * 格式化请求参数
     * 避免日志过长，对大对象进行截断
     *
     * @param args 参数数组
     * @return 格式化后的参数字符串
     */
    private String formatParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "无";
        }

        Predicate<Object> notNull = arg -> arg != null;
        Predicate<Object> notRequest = arg -> !(arg instanceof HttpServletRequest);
        Predicate<Object> notResponse = arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse);
        Function<Object, String> jsonMapper = this::toJsonString;
        Collector<CharSequence, ?, String> joiningCollector = Collectors.joining(", ");
        String params = Arrays.stream(args)
                .filter(notNull)
                .filter(notRequest)
                .filter(notResponse)
                .map(jsonMapper)
                .collect(joiningCollector);

        return truncate(params, 500);
    }

    /**
     * 格式化响应结果
     * 避免日志过长，对大对象进行截断
     *
     * @param result 返回结果
     * @return 格式化后的响应字符串
     */
    private String formatResponse(Object result) {
        if (result == null) {
            return "null";
        }

        String response = toJsonString(result);
        return truncate(response, 500);
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    private String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * 截断字符串
     * 避免日志过长
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...(truncated)";
    }
}
