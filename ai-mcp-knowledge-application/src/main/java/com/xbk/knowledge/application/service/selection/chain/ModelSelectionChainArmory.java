package com.xbk.knowledge.application.service.selection.chain;

import com.xbk.knowledge.application.service.selection.handler.ModelSelectionHandler;

/**
 * 模型选择责任链装配
 *
 * @author xiexu
 */
public interface ModelSelectionChainArmory {

    /**
     * 获取下一个处理器
     *
     * @return 下一个处理器
     */
    ModelSelectionHandler next();

    /**
     * 追加下一个处理器
     *
     * @param next 下一个处理器
     * @return 下一个处理器
     */
    ModelSelectionHandler appendNext(ModelSelectionHandler next);
}
