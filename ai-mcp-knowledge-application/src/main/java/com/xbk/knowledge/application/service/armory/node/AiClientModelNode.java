package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.armory.factory.AiClientArmoryContext;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 模型节点，负责构建 ChatModel。
 * @author sxie
 */
@Component
public class AiClientModelNode extends AbstractAiClientArmoryNode {

    /**
     * 模型提供器工厂。
     */
    private final ModelProviderFactory modelProviderFactory;

    public AiClientModelNode(ModelProviderFactory modelProviderFactory) {
        this.modelProviderFactory = modelProviderFactory;
    }

    @Override
    protected void doHandle(AiClientArmoryContext context) {
        ModelConfig modelConfig = context.getModelConfig();
        ChatModel chatModel = modelProviderFactory
                .getProvider(modelConfig.getModelType())
                .createChatModel(modelConfig);
        context.setChatModel(chatModel);
    }
}
