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
 * OpenAI 模型提供者
 * 封装 OpenAI 模型的创建和调用
 *
 * 职责：模型调用实现，用于适配具体厂商 SDK
 * @author xiexu
 */
@Slf4j
@Component
public class OpenAIModelProvider implements ModelProvider {

    private ChatModel createChatModel(ModelConfig config) {
        try {
            // 规范化 baseUrl，去掉可能存在的 /v1/chat/completions 后缀
            String baseUrl = config.getBaseUrl();
            String normalizedBaseUrl = normalizeBaseUrl(baseUrl);

            log.info("创建 OpenAI 模型 - 原始 baseUrl: {}, 规范化后: {}", baseUrl, normalizedBaseUrl);

            // 创建 OpenAI API 客户端
            String apiKey = config.getApiKey();
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(normalizedBaseUrl)
                    .apiKey(apiKey)
                    .build();

            // 创建聊天选项
            String modelName = config.getModelName();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelName)
                    .build();

            // 创建聊天模型
            return OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(options)
                    .build();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("创建 OpenAI 模型失败: {}", errorMessage, e);
            throw new RuntimeException("创建 OpenAI 模型失败", e);
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
        return ModelType.OPENAI;
    }

    /**
     * 规范化 baseUrl
     * 自动去除可能导致路径重复的后缀，确保与 Spring AI 的 OpenAiApi 兼容
     *
     * Spring AI 的 OpenAiApi 会自动在 baseUrl 后拼接 /v1/chat/completions
     * 因此需要去除用户可能传入的以下后缀：
     * - /v1/chat/completions
     * - /v1
     * - 末尾的斜杠
     *
     * @param baseUrl 原始 baseUrl
     * @return 规范化后的 baseUrl
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }

        // 去掉首尾空格
        String normalized = baseUrl.trim();

        // 去掉末尾的斜杠
        while (normalized.endsWith("/")) {
            int length = normalized.length();
            normalized = normalized.substring(0, length - 1);
        }

        // 检查并去除 /v1/chat/completions 后缀
        if (normalized.endsWith("/v1/chat/completions")) {
            int length = normalized.length();
            int suffixLength = "/v1/chat/completions".length();
            normalized = normalized.substring(0, length - suffixLength);
            log.info("检测到 baseUrl 包含 /v1/chat/completions 后缀，已自动去除。原始: {}, 规范化后: {}", baseUrl, normalized);
        }
        // 检查并去除 /v1 后缀（避免路径重复）
        else if (normalized.endsWith("/v1")) {
            int length = normalized.length();
            int suffixLength = "/v1".length();
            normalized = normalized.substring(0, length - suffixLength);
            log.info("检测到 baseUrl 包含 /v1 后缀，已自动去除。原始: {}, 规范化后: {}", baseUrl, normalized);
        }

        return normalized;
    }

    @Override
    public boolean isHealthy(ModelConfig config) {
        try {
            // 简单的健康检查：尝试创建客户端
            createChatModel(config);
            return true;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.warn("OpenAI 模型健康检查失败: {}", errorMessage);
            return false;
        }
    }
}
