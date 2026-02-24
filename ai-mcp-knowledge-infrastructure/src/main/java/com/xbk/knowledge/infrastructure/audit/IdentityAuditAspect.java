package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.application.service.app.IdentityContextService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.json.JsonMapUtils;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 身份域审计切面。
 *
 * 职责：基础设施审计能力，用于自动记录身份域关键写操作。
 *
 * @author sxie
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdentityAuditAspect {

    private static final int SUCCESS = 1;
    private static final int FAILED = 0;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;
    private static final String SENSITIVE_MASK = "***";
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "passwordHash",
            "apiKey",
            "token",
            "accessToken",
            "refreshToken"
    );

    private final IdentityAuditLogService identityAuditLogService;
    private final ObjectMapper objectMapper;
    private final IdentityContextService identityContextService;

    /**
     * 拦截身份域关键写操作并落审计事件。
     *
     * @param joinPoint 切点
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around(
            "execution(* com.xbk.knowledge.trigger.http.AuthController.login(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AuthController.logout(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.UserIdentityController.create(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.UserIdentityController.update(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.UserIdentityController.resetPassword(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.UserIdentityController.grantRoles(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.RoleController.create(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.RoleController.update(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.RoleController.grantPermissions(..))"
    )
    /**
     * 拦截身份与权限写操作并记录审计日志。
     *
     * @param joinPoint 切点上下文。
     * @return 返回 Object 数据。
     */
    public Object aroundIdentityWriteOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String requestId = TraceIdUtils.getOrCreateTraceId();
        HttpServletRequest request = currentRequest();
        Object[] args = joinPoint.getArgs();
        Long operatorId = resolveOperatorId();
        String resourceType = resolveResourceType(request);
        String resourceId = resolveResourceId(args);
        String action = joinPoint.getSignature().getName();
        String eventType = resolveEventType(request);
        String sourceIp = resolveSourceIp(request);
        String userAgent = request == null ? null : request.getHeader("User-Agent");
        String oldValue = toJsonSafe(buildOldValue(args, eventType));
        Object result = null;
        int executeResult = SUCCESS;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            if (!isSuccess(result)) {
                executeResult = FAILED;
                errorMessage = extractResultMessage(result);
            }
            return result;
        } catch (Throwable throwable) {
            executeResult = FAILED;
            errorMessage = throwable.getMessage();
            throw throwable;
        } finally {
            try {
                long costMs = System.currentTimeMillis() - start;
                String newValue = toJsonSafe(buildNewValue(extractResultData(result), eventType, executeResult));
                SysAuditEvent event = SysAuditEvent.builder()
                        .operatorId(operatorId)
                        .operatorType(operatorId == null ? "system" : "user")
                        .eventType(eventType)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .action(action)
                        .requestId(requestId)
                        .sourceIp(sourceIp)
                        .userAgent(userAgent)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .result(executeResult)
                        .errorMessage(truncate(errorMessage, ERROR_MESSAGE_MAX_LENGTH))
                        .costMs(costMs)
                        .occurredAt(LocalDateTime.now())
                        .build();
                identityAuditLogService.record(event);
            } catch (Exception ex) {
                log.error("记录身份域审计失败，action: {}", action, ex);
            }
        }
    }

    /**
     * 判断接口调用是否成功。
     *
     * @param result 接口返回对象
     * @return 是否成功
     */
    private boolean isSuccess(Object result) {
        if (!(result instanceof Result<?> wrapper)) {
            return false;
        }
        return Objects.equals(wrapper.getCode(), 200);
    }

    /**
     * 提取统一响应消息。
     *
     * @param result 接口返回对象
     * @return 消息
     */
    private String extractResultMessage(Object result) {
        if (result instanceof Result<?> wrapper) {
            return wrapper.getMessage();
        }
        return null;
    }

    /**
     * 提取统一响应 data。
     *
     * @param result 接口返回对象
     * @return 数据对象
     */
    private Object extractResultData(Object result) {
        if (result instanceof Result<?> wrapper) {
            return wrapper.getData();
        }
        return null;
    }

    /**
     * 构造旧值快照。
     *
     * @param args 入参数组
     * @return 旧值快照对象
     */
    private Map<String, Object> buildOldValue(Object[] args, String eventType) {
        Map<String, Object> snapshot = new HashMap<>();
        if (args == null || args.length == 0) {
            return snapshot;
        }
        Object requestObject = args[0];
        Map<String, Object> requestMap = convertToMap(requestObject);
        sanitizeSensitiveValues(requestMap);
        snapshot.put("request", requestMap);
        return snapshot;
    }

    /**
     * 构造新值快照。
     *
     * @param resultData 响应 data
     * @param eventType 事件类型
     * @param executeResult 执行结果
     * @return 新值快照对象
     */
    private Map<String, Object> buildNewValue(Object resultData, String eventType, int executeResult) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("result", executeResult);
        if ("authz".equals(eventType)) {
            return snapshot;
        }
        Object safeResultData = resultData;
        if (resultData != null) {
            Map<String, Object> resultMap = convertToMap(resultData);
            sanitizeSensitiveValues(resultMap);
            safeResultData = resultMap;
        }
        snapshot.put("data", safeResultData);
        return snapshot;
    }

    /**
     * 安全序列化 JSON。
     *
     * @param value 对象
     * @return JSON 字符串
     */
    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 获取当前请求。
     *
     * @return HTTP 请求
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * 解析操作人ID。
     *
     * @return 操作人ID
     */
    private Long resolveOperatorId() {
        try {
            if (!identityContextService.isLogin()) {
                return null;
            }
            return identityContextService.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析资源类型。
     *
     * @param request HTTP 请求
     * @return 资源类型
     */
    private String resolveResourceType(HttpServletRequest request) {
        if (request == null) {
            return "identity";
        }
        String uri = request.getRequestURI();
        if (uri.contains("/auth")) {
            return "auth";
        }
        if (uri.contains("/roles")) {
            return "role";
        }
        if (uri.contains("/orgs")) {
            return "org";
        }
        if (uri.contains("/users")) {
            return "user";
        }
        return "identity";
    }

    /**
     * 解析事件类型。
     *
     * @param request HTTP 请求
     * @return 事件类型
     */
    private String resolveEventType(HttpServletRequest request) {
        if (request == null) {
            return "identity";
        }
        String uri = request.getRequestURI();
        if (uri.contains("/auth")) {
            return "authz";
        }
        return "identity";
    }

    /**
     * 解析资源 ID。
     *
     * @param args 入参数组
     * @return 资源ID
     */
    private String resolveResourceId(Object[] args) {
        Long id = extractLongField(args, "roleId");
        if (id != null) {
            return String.valueOf(id);
        }
        id = extractLongField(args, "userId");
        if (id != null) {
            return String.valueOf(id);
        }
        id = extractLongField(args, "id");
        if (id != null) {
            return String.valueOf(id);
        }
        String username = extractStringField(args, "username");
        if (username != null) {
            return username;
        }
        return "N/A";
    }

    /**
     * 解析来源 IP。
     *
     * @param request HTTP 请求
     * @return IP
     */
    private String resolveSourceIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 通过反射提取 Long 字段。
     *
     * @param args 入参数组
     * @param fieldName 字段名
     * @return Long 值
     */
    private Long extractLongField(Object[] args, String fieldName) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object firstArg = args[0];
        if (firstArg == null) {
            return null;
        }
        Object value = extractFieldValue(firstArg, fieldName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 通过反射提取 String 字段。
     *
     * @param args 入参数组
     * @param fieldName 字段名
     * @return 字符串值
     */
    private String extractStringField(Object[] args, String fieldName) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object firstArg = args[0];
        if (firstArg == null) {
            return null;
        }
        Object value = extractFieldValue(firstArg, fieldName);
        if (value instanceof String text) {
            return text;
        }
        return null;
    }

    /**
     * 通过反射提取字段值。
     *
     * @param target 目标对象
     * @param fieldName 字段名
     * @return 字段值
     */
    private Object extractFieldValue(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将对象转换为 Map 结构。
     *
     * @param value 原对象
     * @return Map
     */
    private Map<String, Object> convertToMap(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        return new HashMap<>(JsonMapUtils.convertToMap(objectMapper, value));
    }

    /**
     * 截断字符串。
     *
     * @param text 原文
     * @param maxLength 最大长度
     * @return 截断后文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * 递归脱敏敏感字段。
     *
     * @param sourceMap 原始 Map
     */
    private void sanitizeSensitiveValues(Map<String, Object> sourceMap) {
        if (sourceMap == null || sourceMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSensitiveField(key)) {
                entry.setValue(SENSITIVE_MASK);
                continue;
            }
            if (value instanceof Map<?, ?> nestedMap) {
                Map<String, Object> normalizedMap = convertToMap(nestedMap);
                sanitizeSensitiveValues(normalizedMap);
                entry.setValue(normalizedMap);
                continue;
            }
            if (value instanceof List<?> nestedList) {
                sanitizeSensitiveList(nestedList);
            }
        }
    }

    /**
     * 脱敏敏感字段列表。
     *
     * @param values 待脱敏值列表。
     */
    @SuppressWarnings("unchecked")
    private void sanitizeSensitiveList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<Object> mutableList = (List<Object>) values;
        for (int i = 0; i < mutableList.size(); i++) {
            Object item = mutableList.get(i);
            if (item instanceof Map<?, ?> nestedMap) {
                Map<String, Object> normalizedMap = convertToMap(nestedMap);
                sanitizeSensitiveValues(normalizedMap);
                mutableList.set(i, normalizedMap);
                continue;
            }
            if (item instanceof List<?> nestedList) {
                sanitizeSensitiveList(nestedList);
            }
        }
    }

    /**
     * 判断是否为敏感字段。
     *
     * @param fieldName 字段名称。
     * @return 返回是否满足业务条件。
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        for (String sensitiveField : SENSITIVE_FIELDS) {
            if (fieldName.equalsIgnoreCase(sensitiveField)) {
                return true;
            }
        }
        return false;
    }
}
