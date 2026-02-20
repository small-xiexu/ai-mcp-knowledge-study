package com.xbk.knowledge.application.service.selection.handler;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selector.ModelSelector;
import com.xbk.knowledge.application.service.selection.chain.AbstractModelSelectionHandler;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认策略选择处理器
 * 用于兜底处理未命中任何显式策略的情况
 *
 * 设计模式：责任链节点（默认兜底）
 * 职责：作为最后节点兜底处理，确保责任链始终可用
 * @author sxie
 */
@Component
public class DefaultSelectionHandler extends AbstractModelSelectionHandler {

    /**
     * 默认策略依赖模型选择器兜底输出
     */
    @Autowired
    private ModelSelector modelSelector;

    /**
     * 兜底节点始终支持，保证责任链有最终落点。
     */
    @Override
    public boolean supports(AICallCommand request) {
        return true;
    }

    /**
     * 兜底处理，不再向下传递。
     */
    @Override
    protected ModelSelectionDecision doSelect(AICallCommand request) {
        ModelConfig selectedModel = modelSelector.selectByQualityPriority();
        return ModelSelectionDecision.byModel(selectedModel);
    }
}
