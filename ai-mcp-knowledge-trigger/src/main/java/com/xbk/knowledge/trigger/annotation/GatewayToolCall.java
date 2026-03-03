package com.xbk.knowledge.trigger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 网关工具调用日志标记注解
 *
 * 用于标识需要自动管理 gatewayToolCallId MDC 上下文的方法
 *
 * 职责：标记需要自动填充 gatewayToolCallId 的方法
 *
 * @author sxie
 * @see com.xbk.knowledge.trigger.aspect.GatewayToolCallAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayToolCall {

    /**
     * gatewayId 参数名
     *
     * 用于从方法参数中提取 gatewayId
     *
     * @return gatewayId 参数名
     */
    String gatewayIdParam() default "gatewayId";

    /**
     * toolName 参数名
     *
     * 用于从方法参数中提取 toolName
     *
     * @return toolName 参数名
     */
    String toolNameParam() default "toolName";
}
