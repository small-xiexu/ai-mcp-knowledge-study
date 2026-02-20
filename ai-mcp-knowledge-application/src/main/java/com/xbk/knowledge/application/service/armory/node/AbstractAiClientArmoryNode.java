package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;

/**
 * AI 客户端装配节点抽象基类。
 */
public abstract class AbstractAiClientArmoryNode implements AiClientArmoryNode {

    private AiClientArmoryNode next;

    @Override
    public final void handle(DefaultAiClientArmoryStrategyFactory.DynamicContext context) {
        doHandle(context);
        if (next != null) {
            next.handle(context);
        }
    }

    @Override
    public void setNext(AiClientArmoryNode next) {
        this.next = next;
    }

    /**
     * 当前节点逻辑。
     *
     * @param context 动态上下文
     */
    protected abstract void doHandle(DefaultAiClientArmoryStrategyFactory.DynamicContext context);
}

