package com.xbk.knowledge.infrastructure.provider;

import com.xbk.knowledge.application.provider.ModelProvider;
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
 * Google Gemini 模型提供者
 * 封装 Google Gemini 模型的创建和调用
 *
 * 实现说明：
 * - 使用 OpenAI 兼容协议调用 Gemini，避免 Spring AI 的 GoogleGenAiChatModel 在处理工具调用时的 bug
 * - Spring AI 1.1.2 版本的 GoogleGenAiChatModel 在处理工具调用响应时，当 text 字段为空时会抛出 NoSuchElementException
 * - 通过 OpenAI 兼容协议可以绕过这个问题，同时保持完整的功能支持（工具调用、MCP 集成等）
 *
 * 职责：模型调用实现，用于适配具体厂商 SDK
 * @author xiexu
 */
@Slf4j
@Component
public class GeminiModelProvider implements ModelProvider {

    private ChatModel createChatModel(ModelConfig config) {
        try {
            log.info("创建 Gemini 模型（通过 OpenAI 兼容协议）: {}", config.getModelName());

            // 使用 OpenAI 兼容协议调用 Gemini
            // 这样可以避免 Spring AI 的 GoogleGenAiChatModel 的 bug
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
            log.error("创建 Gemini 模型失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建 Gemini 模型失败", e);
        }
    }

    @Override
    public ChatClient createChatClient(ModelConfig config) {
        ChatModel chatModel = createChatModel(config);
        return ChatClient.builder(chatModel).build();
    }

    @Override
    public ModelType getModelType() {
        return ModelType.GEMINI;
    }

    @Override
    public boolean isHealthy(ModelConfig config) {
        try {
            // 简单的健康检查：尝试创建客户端
            createChatModel(config);
            return true;
        } catch (Exception e) {
            log.warn("Gemini 模型健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
