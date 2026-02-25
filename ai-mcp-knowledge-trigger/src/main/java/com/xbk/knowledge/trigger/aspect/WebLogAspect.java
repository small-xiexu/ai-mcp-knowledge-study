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
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Web 请求日志切面
 * 统一记录所有 HTTP 接口的请求/响应/耗时信息
 *
 * 设计目标
 * 1、 日志结构稳定，方便后续接入日志平台检索
 * 2、 请求/响应尽量保持 JSON 结构，避免解析困难
 * 3、 避免大对象刷屏，通过截断包装保持合法 JSON
 *
 * 职责：接口层横切逻辑，用于统一日志与追踪
 *
 * @author sxie
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WebLogAspect {
    /**
     * JSON 序列化组件，用于统一格式化请求/响应日志内容。
     */
    private final ObjectMapper objectMapper;

    /**
     * 执行 Web 请求日志切面处理。
     */
    @Pointcut("execution(public * com.xbk.knowledge.trigger.http..*Controller.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知记录请求和响应信息
     *
     * 统一请求/响应日志结构，确保排障时可关联 traceId 与耗时。
     *
     * @throws Throwable 异常
     * 
     * @param joinPoint 连接点
     * @return 方法返回值
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录起始时间，统一计算接口耗时
        long startTime = TimeCostUtils.start();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        // 获取或生成 traceId，保证日志可串联
        String traceId = TraceIdUtils.getOrCreateTraceId();
        // 将 traceId 写入日志上下文，便于日志平台串联请求
        RequestContext requestContext = buildRequestContext(joinPoint, request, traceId);

        Object result = null;
        Throwable exception = null;
        try {
            // 执行目标方法，捕获正常响应
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            // 记录异常并交由上层处理
            exception = e;
            throw e;
        } finally {
            // 统一输出请求/响应日志（含耗时）
            long costTime = TimeCostUtils.costMillis(startTime);
            logRequestAndResponse(requestContext, result, exception, costTime);
        }
    }

    /**
     * 构建请求上下文
     * 使用简化类名与方法名，避免日志过长且便于定位
     *
     * 保持日志字段稳定且可读，降低检索成本。
     * 
     * @param joinPoint AOP 切点信息。
     * @param request HTTP 请求。
     * @param traceId 标识 ID。
     * @return 请求上下文。
     */
    private RequestContext buildRequestContext(ProceedingJoinPoint joinPoint, HttpServletRequest request, String traceId) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        int lastDotIndex = className.lastIndexOf('.');
        int classNameStart = lastDotIndex + 1;
        String simpleClassName = className.substring(classNameStart);

        // 组装请求 URL 与 URI，便于定位具体接口
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";
        String requestUrl = buildRequestUrl(request);

        return new RequestContext(
                traceId,
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
     * 避免多条日志分散导致上下文难以拼接。
     * 
     * @param requestContext 请求上下文
     * @param result 返回结果
     * @param exception 异常信息
     * @param costTime 耗时（毫秒）
     */
    private void logRequestAndResponse(RequestContext requestContext, Object result,
                              Throwable exception, long costTime) {
        // 统一日志前缀，保持字段稳定便于检索
        String prefix = String.format("ClassName:%s.%s url=%s",
                requestContext.controller,
                requestContext.method,
                requestContext.requestUrl);
        String requestJson = formatRequestJson(requestContext.args);
        if (exception != null) {
            // 异常场景输出错误信息，响应置空
            String exceptionMessage = exception.getMessage();
            log.error("{} request={} response=null duration={} error={}",
                    prefix,
                    requestJson,
                    costTime,
                    exceptionMessage);
        } else {
            // 正常场景输出响应内容
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
     * 保证日志可解析且不因大对象导致日志爆量。
     * 
     * @param args 方法参数数组
     * @return 格式化后的参数字符串
     */
    private String formatRequestJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "null";
        }

        Function<Object, String> jsonMapper = this::toJsonString;
        String params = Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof HttpServletResponse))
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
     * 兼顾可读性与机器可解析性。
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

    /**
     * 截断过长 JSON 字符串。
     * 
     * @param json JSON 字符串。
     * @param maxLength 最大长度。
     * @return JSON 字符串。
     */
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

    /**
     * 构建请求地址。
     * 
     * @param request HTTP 请求。
     * @return 构建后的请求地址。
     */
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
     * 日志记录不应影响接口主流程。
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
     * @param str 原始字符串
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
     * @author sxie
     */
    private static class RequestContext {

        /**
         * 链路追踪 ID。
         */
        private final String traceId;

        /**
         * 控制器名称。
         */
        private final String controller;

        /**
         * 控制器方法名称。
         */
        private final String method;

        /**
         * 请求 URI。
         */
        private final String requestUri;

        /**
         * 请求完整 URL。
         */
        private final String requestUrl;

        /**
         * 方法参数快照。
         */
        private final Object[] args;

        /**
         * 构建请求上下文快照对象。
         * 
         * @param traceId 链路追踪ID。
         * @param controller 控制器名称。
         * @param method 方法名称。
         * @param requestUri 请求URI。
         * @param requestUrl 请求URL。
         * @param args 方法参数数组。
         */
        private RequestContext(String traceId, String controller, String method, String requestUri, String requestUrl,
                               Object[] args) {
            this.traceId = traceId;
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
     * @author sxie
     */
    @Getter
    private static class TruncatedPayload {

        /**
         * 是否发生截断。
         */
        private final boolean truncated;

        /**
         * 截断后的字符串值。
         */
        private final String value;

        private TruncatedPayload(boolean truncated, String value) {
            this.truncated = truncated;
            this.value = value;
        }
    }
}
