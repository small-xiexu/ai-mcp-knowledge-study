package com.xbk.knowledge.config.ai;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记“全局必备”的 CallAdvisor。
 *
 * 说明：ChatClientEnhancer 只会默认注入带该注解的 Advisor；
 * 其他 Advisor 通过“可配置 Advisor 资产”在运行时按绑定装配。
 
  * @author xiexu
  */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalChatAdvisor {
}

