package com.xbk.knowledge.application.fallback.chain;

import com.xbk.knowledge.application.fallback.policy.ModelCallPolicy;

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
     * 获取下一个策略节点
     *
     * @return 下一个策略节点
     */
    ModelCallPolicy next();

    /**
     * 追加下一个策略节点
     *
     * @param next 下一个策略节点
     * @return 下一个策略节点
     */
    ModelCallPolicy appendNext(ModelCallPolicy next);
}
