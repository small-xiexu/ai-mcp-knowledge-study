package com.xbk.knowledge.application.service.selection.handler;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selector.ModelSelector;
import com.xbk.knowledge.application.service.selection.chain.AbstractModelSelectionHandler;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import com.xbk.knowledge.types.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 显式策略选择处理器
 * 当请求明确指定策略时优先处理
 *
 * 设计模式：责任链节点（显式策略优先）
 * 职责：命中显式策略直接处理，未命中则交给下一个节点
 * @author xiexu
 */
@Component
public class ExplicitStrategySelectionHandler extends AbstractModelSelectionHandler {

    /**
     * 显式策略依赖模型选择器完成模型确定
     */
    @Autowired
    private ModelSelector modelSelector;

    /**
     * 对外暴露 supports 作为判断入口，未命中则交由下一个节点处理。
     */
    @Override
    public boolean supports(AICallCommand request) {
        return request.getStrategy() != null;
    }

    /**
     * 处理显式策略，未命中时调用 next 继续责任链。
     */
    @Override
    protected ModelSelectionDecision doSelect(AICallCommand request) {
        if (!supports(request)) {
            return next().select(request);
        }
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
