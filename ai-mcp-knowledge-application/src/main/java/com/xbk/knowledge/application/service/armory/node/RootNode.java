package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 根节点，负责入参校验与上下文初始化。
 */
@Slf4j
@Component
public class RootNode extends AbstractAiClientArmoryNode {

    @Override
    protected void doHandle(DefaultAiClientArmoryStrategyFactory.DynamicContext context) {
        if (context == null) {
            throw new IllegalArgumentException("AI 客户端装配上下文不能为空");
        }
        ModelConfig modelConfig = context.getModelConfig();
        if (modelConfig == null || modelConfig.getModelType() == null) {
            throw new IllegalArgumentException("模型配置不完整，无法装配 ChatClient");
        }
        context.normalizeExtraAdvisors();
        context.setResolvedEnableTools(context.isRequestedEnableTools());
        log.debug("装配根节点通过，modelId={}, modelType={}",
                modelConfig.getId(), modelConfig.getModelType());
    }
}
