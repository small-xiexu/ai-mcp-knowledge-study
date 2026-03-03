package com.xbk.knowledge.trigger.aspect;

import com.xbk.knowledge.trigger.annotation.GatewayToolCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.UUID;

/**
 * 网关工具调用日志切面
 *
 * 自动管理 gatewayToolCallId 的 MDC 上下文，统一记录工具调用日志
 *
 * 职责：横切网关工具调用方法，自动处理 MDC 和日志记录
 *
 * @author sxie
 * @see com.xbk.knowledge.trigger.annotation.GatewayToolCall
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GatewayToolCallAspect {

    /**
     * MDC 中 gatewayToolCallId 的键名。
     */
    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";

    /**
     * 环绕通知：自动管理 MDC 上下文并记录工具调用日志
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(gatewayToolCall)")
    public Object around(ProceedingJoinPoint joinPoint, GatewayToolCall gatewayToolCall) throws Throwable {
        // 生成 callId
        String callId = generateCallId();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 从参数中提取 gatewayId 和 toolName
        String gatewayId = extractParameter(joinPoint, gatewayToolCall.gatewayIdParam());
        String toolName = extractParameter(joinPoint, gatewayToolCall.toolNameParam());

        // 记录 MDC
        String previousCallId = setCallId(callId);
        try {
            log.info("gateway_tool_call source=API stage=start callId={} gatewayId={} toolName={}",
                    callId, gatewayId, toolName);

            // 执行目标方法
            Object result = joinPoint.proceed();

            log.info("gateway_tool_call source=API stage=end callId={} gatewayId={} toolName={} success=true",
                    callId, gatewayId, toolName);

            return result;
        } catch (Throwable e) {
            log.warn("gateway_tool_call source=API stage=end callId={} gatewayId={} toolName={} success=false error={}",
                    callId, gatewayId, toolName, e.getMessage());
            throw e;
        } finally {
            // 恢复之前的 callId
            restoreCallId(previousCallId);
        }
    }

    /**
     * 生成调用 ID
     *
     * @return 调用 ID
     */
    private String generateCallId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 从方法参数中提取指定名称的参数值
     *
     * @param joinPoint 连接点
     * @param paramName 参数名
     * @return 参数值
     */
    private String extractParameter(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (paramName.equals(parameters[i].getName())) {
                Object value = args[i];
                return value != null ? value.toString() : null;
            }

            // 支持从 DTO 对象中提取
            if (args[i] != null) {
                try {
                    String getterName = "get" + capitalize(paramName);
                    var method = args[i].getClass().getMethod(getterName);
                    Object value = method.invoke(args[i]);
                    return value != null ? value.toString() : null;
                } catch (Exception ignored) {
                    // 忽略，继续尝试其他方式
                }
            }
        }

        return null;
    }

    /**
     * 设置 callId 到 MDC，返回之前的值
     *
     * @param callId 调用 ID
     * @return 之前的 callId
     */
    private String setCallId(String callId) {
        String previous = (String) org.slf4j.MDC.get(CALL_ID_MDC_KEY);
        org.slf4j.MDC.put(CALL_ID_MDC_KEY, callId);
        return previous;
    }

    /**
     * 恢复之前的 callId
     *
     * @param previousCallId 之前的 callId
     */
    private void restoreCallId(String previousCallId) {
        if (StringUtils.hasText(previousCallId)) {
            org.slf4j.MDC.put(CALL_ID_MDC_KEY, previousCallId);
        } else {
            org.slf4j.MDC.remove(CALL_ID_MDC_KEY);
        }
    }

    /**
     * 字符串首字母大写
     *
     * @param str 原始字符串
     * @return 首字母大写后的字符串
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
