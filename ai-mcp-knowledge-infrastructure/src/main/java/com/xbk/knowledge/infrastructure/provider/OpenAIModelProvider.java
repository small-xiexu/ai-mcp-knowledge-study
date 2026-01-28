package com.xbk.knowledge.infrastructure.provider;
import com.xbk.knowledge.domain.provider.ModelProvider;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * OpenAI 模型提供者
 * 封装 OpenAI 模型的创建和调用
 *
 * @author xiexu
 */
@Component
@Slf4j
public class OpenAIModelProvider implements ModelProvider {

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        try {
            // 创建 OpenAI API 客户端
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(config.getBaseUrl())
                    .apiKey(config.getApiKey())
                    .build();

            // 创建聊天选项
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(config.getModelName())
                    .build();

            // 创建聊天模型
            return OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(options)
                    .build();
        } catch (Exception e) {
            log.error("创建 OpenAI 模型失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建 OpenAI 模型失败", e);
        }
    }

    @Override
    public ChatClient createChatClient(ModelConfig config) {
        ChatModel chatModel = createChatModel(config);
        return ChatClient.builder(chatModel).build();
    }

    @Override
    public ModelType getModelType() {
        return ModelType.OPENAI;
    }

    @Override
    public boolean isHealthy(ModelConfig config) {
        try {
            // 简单的健康检查：尝试创建客户端
            createChatModel(config);
            return true;
        } catch (Exception e) {
            log.warn("OpenAI 模型健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
