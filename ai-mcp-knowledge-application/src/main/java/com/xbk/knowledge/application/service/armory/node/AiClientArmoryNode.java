package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;

/**
 * AI 客户端装配节点接口。
 */
public interface AiClientArmoryNode {

    /**
     * 执行当前节点。
     *
     * @param context 动态上下文
     */
    void handle(DefaultAiClientArmoryStrategyFactory.DynamicContext context);

    /**
     * 设置下一个节点。
     *
     * @param next 下一个节点
     */
    void setNext(AiClientArmoryNode next);
}

