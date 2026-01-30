package com.xbk.knowledge.application.fallback;

/**
 * 调用策略拦截器
 * 通过责任链方式叠加重试/熔断/日志等能力
 *
 * 设计模式：责任链节点（Interceptor）
 * 职责：应用层拦截器，用于解耦横切逻辑并避免流程膨胀
 * @author xiexu
 */
public interface ModelCallPolicy {

    /**
     * 执行策略逻辑
     *
     * @param context 调用上下文
     * @param chain   策略链
     * @return 执行结果
     */
    ModelCallOutcome apply(ModelCallContext context, ModelCallPolicyChain chain);
}
