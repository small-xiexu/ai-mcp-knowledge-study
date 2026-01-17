package com.xbk.knowledge.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Gemini 配置类（Google GenAI SDK 模式）
 * 通过 Google GenAI 协议调用 Gemini 模型
 * <p>
 * 支持自定义 base-url（用于代理服务）
 *
 * @author xiexu
 */
@Configuration
public class GeminiConfig {

    /**
     * 创建 Google GenAI Client
     * 支持自定义 base-url（用于代理或第三方兼容服务）
     */
    @Bean
    public Client googleGenAiClient(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${spring.ai.google.genai.base-url}") String baseUrl) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .baseUrl(baseUrl)
                        .build())
                .build();
    }

    /**
     * 创建 Gemini Chat 模型
     */
    @Bean
    public GoogleGenAiChatModel geminiChatModel(
            Client googleGenAiClient,
            @Value("${spring.ai.google.genai.chat.options.model}") String model) {
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .build();
        return GoogleGenAiChatModel.builder()
                .genAiClient(googleGenAiClient)
                .defaultOptions(options)
                .build();
    }

    /**
     * 创建基于 Gemini 的 ChatClient.Builder
     */
    @Bean("geminiChatClientBuilder")
    public ChatClient.Builder geminiChatClientBuilder(GoogleGenAiChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel);
    }

}
