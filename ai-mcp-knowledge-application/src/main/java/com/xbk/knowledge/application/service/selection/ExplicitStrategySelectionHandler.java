package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.ModelSelector;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 显式策略选择处理器
 * 当请求明确指定策略时优先处理
 *
 * 设计模式：责任链节点（显式策略优先）
 * 职责：保证显式策略优先级最高，并拦截未实现策略
 * @author xiexu
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class ExplicitStrategySelectionHandler implements ModelSelectionHandler {

    private final ModelSelector modelSelector;

    @Override
    public boolean supports(AICallCommand request) {
        return request.getStrategy() != null;
    }

    @Override
    public ModelSelectionDecision select(AICallCommand request) {
        ModelSelectionStrategy strategy = request.getStrategy();
        if (strategy == ModelSelectionStrategy.QUALITY_PRIORITY) {
            ModelConfig selectedModel = modelSelector.selectByQualityPriority();
            return ModelSelectionDecision.byModel(selectedModel);
        }
        String strategyName = strategy.getName();
        String message = "模型选择策略未实现：" + strategyName;
        throw new BusinessException(ResultCode.STRATEGY_NOT_SUPPORTED.getCode(), message);
    }
}
