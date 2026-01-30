package com.xbk.knowledge.application.fallback;

/**
 * 调用策略链
 * 用于按顺序执行策略拦截器，避免调用方显式控制流程
 *
 * 设计模式：责任链调度器
 * 职责：应用层流程组件，用于串联调用策略
 * @author xiexu
 */
public interface ModelCallPolicyChain {

    /**
     * 进入下一段策略或执行器
     *
     * @param context 调用上下文
     * @return 执行结果
     */
    ModelCallOutcome proceed(ModelCallContext context);
}
