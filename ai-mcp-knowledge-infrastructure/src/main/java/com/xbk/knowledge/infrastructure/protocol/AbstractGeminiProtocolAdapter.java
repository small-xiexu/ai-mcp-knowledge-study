package com.xbk.knowledge.infrastructure.protocol;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Gemini 协议抽象适配器
 * 使用 OpenAI 兼容协议调用 Gemini
 *
 * 说明：
 * - Spring AI 1.1.2 的 GoogleGenAiChatModel 在工具调用响应为空时存在异常
 * - 采用 OpenAI 兼容协议可规避该问题并保持功能一致
 *
 * 职责：协议层构建 ChatModel，供具体协议实现复用
 *
 * @author xiexu
 */
@Slf4j
public abstract class AbstractGeminiProtocolAdapter {

    /**
     * 创建基于 Gemini 协议的 ChatModel
     *
     * @param config 模型配置
     * @return ChatModel
     */
    public ChatModel createChatModel(ModelConfig config) {
        String modelName = config.getModelName();
        log.info("创建 Gemini 模型（通过 OpenAI 兼容协议）: {}", modelName);

        String baseUrl = config.getBaseUrl();
        String apiKey = config.getApiKey();
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
