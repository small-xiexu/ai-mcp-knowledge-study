package com.xbk.knowledge.config.ai;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记“全局必备”的 CallAdvisor。
 *
 * 说明：armory 编排节点会默认注入带该注解的 Advisor；
 * 其他 Advisor 通过运行时绑定按需装配。
 *
 * @author sxie
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalChatAdvisor {
}
