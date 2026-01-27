package com.xbk.knowledge.orchestration.provider;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ModelType;
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
 * @author xiexu
 */
@Component
@Slf4j
public class AnthropicModelProvider implements ModelProvider {

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        try {
            // 创建 Anthropic API 客户端
            AnthropicApi anthropicApi = AnthropicApi.builder()
                    .baseUrl(config.getBaseUrl())
                    .apiKey(config.getApiKey())
                    .build();

            // 创建聊天选项
            AnthropicChatOptions options = AnthropicChatOptions.builder()
                    .model(config.getModelName())
                    .build();

            // 创建聊天模型
            return AnthropicChatModel.builder()
                    .anthropicApi(anthropicApi)
                    .defaultOptions(options)
                    .build();
        } catch (Exception e) {
            log.error("创建 Anthropic 模型失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建 Anthropic 模型失败", e);
        }
    }

    @Override
    public ChatClient createChatClient(ModelConfig config) {
        ChatModel chatModel = createChatModel(config);
        return ChatClient.builder(chatModel).build();
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
            log.warn("Anthropic 模型健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
