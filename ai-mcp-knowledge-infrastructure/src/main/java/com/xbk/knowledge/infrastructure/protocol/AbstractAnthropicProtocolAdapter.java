package com.xbk.knowledge.infrastructure.protocol;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Anthropic 协议抽象适配器
 * 统一封装 Anthropic Claude 模型的构建逻辑
 * <p>
 * 职责：协议层构建 ChatModel，供具体协议实现复用
 *
 * @author sxie
 */
public abstract class AbstractAnthropicProtocolAdapter {

    /**
     * 创建基于 Anthropic 协议的 ChatModel
     * <p>
     * 统一封装 Anthropic 协议模型创建逻辑
     * 
     * @param config 配置信息。
     * @return Anthropic 协议对话模型。
     */
    public ChatModel createChatModel(ModelConfig config) {
        String baseUrl = config.getBaseUrl();
        String apiKey = config.getApiKey();
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        String modelName = config.getModelName();
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(modelName)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(options)
                .build();
    }
}
