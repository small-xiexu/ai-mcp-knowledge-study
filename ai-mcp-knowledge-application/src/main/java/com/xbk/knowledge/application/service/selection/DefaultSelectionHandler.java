package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.ModelSelector;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认策略选择处理器
 * 用于兜底处理未命中任何显式策略的情况
 *
 * 设计模式：责任链节点（默认兜底）
 * 职责：确保选择链始终可用，避免空结果
 * @author xiexu
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class DefaultSelectionHandler implements ModelSelectionHandler {

    private final ModelSelector modelSelector;

    /**
     * 对外暴露 supports 作为调用入口，便于上层复用。
     */
    @Override
    public boolean supports(AICallCommand request) {
        return true;
    }

    /**
     * 对外暴露 select 作为调用入口，便于上层复用。
     */
    @Override
    public ModelSelectionDecision select(AICallCommand request) {
        ModelConfig selectedModel = modelSelector.selectByQualityPriority();
        return ModelSelectionDecision.byModel(selectedModel);
    }
}
