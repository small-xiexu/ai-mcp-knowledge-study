package com.xbk.knowledge.infrastructure.protocol;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * OpenAI 兼容协议抽象适配器
 * 统一封装 OpenAI 协议的模型构建逻辑
 *
 * 职责：协议层构建 ChatModel，供具体协议实现复用
 *
 * @author xiexu
 */
@Slf4j
public abstract class AbstractOpenAiProtocolAdapter {

    /**
     * 创建基于 OpenAI 协议的 ChatModel
     *
     * 为什么：统一封装 OpenAI 协议模型创建逻辑
     * 入参：模型配置
     * 出参：ChatModel
     */
    public ChatModel createChatModel(ModelConfig config) {
        /*
         * 目的：规范化 baseUrl，避免路径重复
         */
        String baseUrl = config.getBaseUrl();
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);

        log.info("创建 OpenAI 协议模型 - 原始 baseUrl: {}, 规范化后: {}", baseUrl, normalizedBaseUrl);

        String apiKey = config.getApiKey();
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(apiKey)
                .build();

        String modelName = config.getModelName();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
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
     * 为什么：避免 Spring AI 自动拼接导致重复路径
     * 入参：原始 baseUrl
     * 出参：规范化后的 baseUrl
     */
    protected String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            int length = normalized.length();
            normalized = normalized.substring(0, length - 1);
        }

        if (normalized.endsWith("/v1/chat/completions")) {
            int length = normalized.length();
            int suffixLength = "/v1/chat/completions".length();
            normalized = normalized.substring(0, length - suffixLength);
            log.info("检测到 baseUrl 包含 /v1/chat/completions 后缀，已自动去除。原始: {}, 规范化后: {}", baseUrl, normalized);
        } else if (normalized.endsWith("/v1")) {
            int length = normalized.length();
            int suffixLength = "/v1".length();
            normalized = normalized.substring(0, length - suffixLength);
            log.info("检测到 baseUrl 包含 /v1 后缀，已自动去除。原始: {}, 规范化后: {}", baseUrl, normalized);
        }

        return normalized;
    }
}
