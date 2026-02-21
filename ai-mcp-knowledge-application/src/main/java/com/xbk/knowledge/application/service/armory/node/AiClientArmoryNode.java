package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.AiClientArmoryContext;

/**
 * AI 客户端装配节点接口。
 * @author sxie
 */
public interface AiClientArmoryNode {

    /**
     * 执行当前节点。
     *
     * @param context 动态上下文
     */
    void handle(AiClientArmoryContext context);

    /**
     * 设置下一个节点。
     *
     * @param next 下一个节点
     */
    void setNext(AiClientArmoryNode next);
}
