package com.xbk.knowledge.infrastructure.provider;

import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Anthropic 模型提供者
 * 封装 Anthropic Claude 模型的创建和调用
 *
 * 职责：模型调用实现，用于适配具体厂商 SDK
 * @author xiexu
 */
@Slf4j
@Component
public class AnthropicModelProvider implements ModelProvider {

    private ChatModel createChatModel(ModelConfig config) {
        try {
            // 创建 Anthropic API 客户端
            String baseUrl = config.getBaseUrl();
            String apiKey = config.getApiKey();
            AnthropicApi anthropicApi = AnthropicApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .build();

            // 创建聊天选项
            String modelName = config.getModelName();
            AnthropicChatOptions options = AnthropicChatOptions.builder()
                    .model(modelName)
                    .build();

            // 创建聊天模型
            return AnthropicChatModel.builder()
                    .anthropicApi(anthropicApi)
                    .defaultOptions(options)
                    .build();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("创建 Anthropic 模型失败: {}", errorMessage, e);
            throw new RuntimeException("创建 Anthropic 模型失败", e);
        }
    }

    @Override
    public ChatClient createChatClient(ModelConfig config) {
        ChatModel chatModel = createChatModel(config);
        return ChatClient
                .builder(chatModel)
                .build();
    }

    @Override
    public ModelType getModelType() {
        return ModelType.ANTHROPIC;
    }

    @Override
    public boolean isHealthy(ModelConfig config) {
        try {
            // 简单的健康检查：尝试创建客户端
            createChatModel(config);
            return true;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.warn("Anthropic 模型健康检查失败: {}", errorMessage);
            return false;
        }
    }
}
