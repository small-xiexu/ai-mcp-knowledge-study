package com.xbk.knowledge.application.fallback.policy;

import com.xbk.knowledge.application.fallback.chain.ModelCallPolicyChain;
import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.core.ModelCallOutcome;

/**
 * 调用策略拦截器
 * 通过责任链方式叠加重试/熔断/日志等能力
 *
 * 设计模式：责任链节点（Interceptor）
 * 职责：应用层拦截器，用于解耦横切逻辑并避免流程膨胀
 * @author sxie
 */
public interface ModelCallPolicy extends ModelCallPolicyChain {

    /**
     * 执行策略逻辑
     *
     * @param context 调用上下文
     * @return 执行结果
     */
    ModelCallOutcome apply(ModelCallContext context);
}
