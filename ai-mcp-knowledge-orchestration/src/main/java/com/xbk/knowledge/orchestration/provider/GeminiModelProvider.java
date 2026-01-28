package com.xbk.knowledge.orchestration.provider;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * Google Gemini 模型提供者
 * 封装 Google Gemini 模型的创建和调用
 *
 * @author xiexu
 */
@Component
@Slf4j
public class GeminiModelProvider implements ModelProvider {

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        try {
            // 创建 Google GenAI 客户端
            Client genAiClient = Client.builder()
                    .apiKey(config.getApiKey())
                    .httpOptions(HttpOptions.builder()
                            .baseUrl(config.getBaseUrl())
                            .build())
                    .build();

            // 创建聊天选项
            GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                    .model(config.getModelName())
                    .build();

            // 创建聊天模型
            return GoogleGenAiChatModel.builder()
                    .genAiClient(genAiClient)
                    .defaultOptions(options)
                    .build();
        } catch (Exception e) {
            log.error("创建 Gemini 模型失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建 Gemini 模型失败", e);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GEMINI;
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
