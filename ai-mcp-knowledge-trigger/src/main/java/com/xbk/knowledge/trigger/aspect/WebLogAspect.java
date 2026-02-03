package com.xbk.knowledge.trigger.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xbk.knowledge.types.time.TimeCostUtils;
import lombok.Getter;
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
 * 统一记录所有 HTTP 接口的请求/响应/耗时信息
 *
 * 设计目标：
 * 1) 日志结构稳定，方便后续接入日志平台检索
 * 2) 请求/响应尽量保持 JSON 结构，避免解析困难
 * 3) 避免大对象刷屏，通过截断包装保持合法 JSON
 *
 * 职责：接口层横切逻辑，用于统一日志与追踪
 *
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
     * 为什么：统一请求/响应日志结构，确保排障时可关联 traceId 与耗时。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        
        long startTime = TimeCostUtils.start();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String traceId = TraceIdUtils.getOrCreateTraceId();

        RequestContext requestContext = buildRequestContext(joinPoint, request);

        
        Object result = null;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long costTime = TimeCostUtils.costMillis(startTime);
            logRequestAndResponse(requestContext, result, exception, costTime);
        }
    }

    /**
     * 构建请求上下文
     * 使用简化类名与方法名，避免日志过长且便于定位
     *
     * 为什么：保持日志字段稳定且可读，降低检索成本。
     */
    private RequestContext buildRequestContext(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        int lastDotIndex = className.lastIndexOf('.');
        int classNameStart = lastDotIndex + 1;
        String simpleClassName = className.substring(classNameStart);

        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";
        String requestUrl = buildRequestUrl(request);

        return new RequestContext(
                simpleClassName,
                methodName,
                requestUri,
                requestUrl,
                joinPoint.getArgs()
        );
    }

    /**
     * 记录请求与响应信息
     * 保持一条日志输出，便于 ELK 聚合检索
     *
     * 为什么：避免多条日志分散导致上下文难以拼接。
     *
     * @param requestContext 请求上下文
     * @param result    返回结果
     * @param exception 异常信息
     * @param costTime  耗时（毫秒）
     */
    private void logRequestAndResponse(RequestContext requestContext, Object result,
                              Throwable exception, long costTime) {
        String prefix = String.format("ClassName:%s.%s url=%s",
                requestContext.controller,
                requestContext.method,
                requestContext.requestUrl);
        String requestJson = formatRequestJson(requestContext.args);
        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            log.error("{} request={} response=null duration={} error={}",
                    prefix,
                    requestJson,
                    costTime,
                    exceptionMessage);
        } else {
            String responseJson = formatResponseJson(result);
            log.info("{} request={} response={} duration={}",
                    prefix,
                    requestJson,
                    responseJson,
                    costTime);
        }
    }

    /**
     * 格式化请求参数
     * 避免日志过长，对大对象进行截断
     * 过滤 Servlet 原生对象，避免日志污染和序列化异常
     *
     * 为什么：保证日志可解析且不会因大对象导致日志爆量。
     *
     * @param args 参数数组
     * @return 格式化后的参数字符串
     */
    private String formatRequestJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "null";
        }

        Function<Object, String> jsonMapper = this::toJsonString;
        String params = Arrays.stream(args)
                .filter(arg -> arg != null)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .map(jsonMapper)
                .collect(Collectors.joining(","));

        String json = params.contains(",") ? "[" + params + "]" : params;
        return truncateJsonString(json, 500);
    }

    /**
     * 格式化响应结果
     * 避免日志过长，对大对象进行截断
     * 截断时保持 JSON 结构，方便下游解析
     *
     * 为什么：兼顾可读性与机器可解析性。
     *
     * @param result 返回结果
     * @return 格式化后的响应 JSON
     */
    private String formatResponseJson(Object result) {
        if (result == null) {
            return "null";
        }

        String json = toJsonString(result);
        return truncateJsonString(json, 500);
    }

    private String truncateJsonString(String json, int maxLength) {
        if (json == null) {
            return "null";
        }
        if (json.length() <= maxLength) {
            return json;
        }
        
        TruncatedPayload payload = new TruncatedPayload(true, truncate(json, maxLength));
        return toJsonString(payload);
    }

    private String buildRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        StringBuffer url = request.getRequestURL();
        if (url == null) {
            return "UNKNOWN";
        }
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return url.toString();
        }
        
        return url.append('?').append(query).toString();
    }

    /**
     * 对象转 JSON 字符串
     * 序列化失败时回退为 toString，避免影响主流程
     *
     * 为什么：日志记录不应影响接口主流程。
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

    /**
     * 请求日志上下文
     * 仅保留日志必要字段，避免持有完整请求对象导致内存占用与序列化风险
     *
     * @author xiexu
     */
    private static class RequestContext {
        
        private final String controller;
        private final String method;
        private final String requestUri;
        private final String requestUrl;
        private final Object[] args;

        private RequestContext(String controller, String method, String requestUri, String requestUrl, Object[] args) {
            this.controller = controller;
            this.method = method;
            this.requestUri = requestUri;
            this.requestUrl = requestUrl;
            this.args = args != null ? args : new Object[0];
        }
    }

    /**
     * 截断响应包装
     * 通过结构化包装保持 JSON 合法性，便于日志平台稳定解析
     *
     * @author xiexu
     */
    @Getter
    private static class TruncatedPayload {
        
        private final boolean truncated;
        private final String value;

        private TruncatedPayload(boolean truncated, String value) {
            this.truncated = truncated;
            this.value = value;
        }
    }
}
