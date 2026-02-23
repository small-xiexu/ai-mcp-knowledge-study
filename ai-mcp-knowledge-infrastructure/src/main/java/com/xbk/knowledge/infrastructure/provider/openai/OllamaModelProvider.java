package com.xbk.knowledge.infrastructure.provider.openai;

import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Component;

/**
 * Ollama 模型提供者
 * 封装 Ollama 模型的创建和调用
 *
 * 职责：模型调用实现，用于适配具体厂商 SDK
 * @author sxie
 */
@Slf4j
@Component
public class OllamaModelProvider implements ModelProvider {

    /**
     * 构建 ChatModel
     *
     * 为什么：统一封装 Ollama SDK 构建过程
     * 入参：模型配置
     * 出参：ChatModel
     */
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        try {
            String baseUrl = config.getBaseUrl();
            String modelName = config.getModelName();

            log.info("创建 Ollama 模型 - baseUrl: {}, model: {}", baseUrl, modelName);

            // 构建 Ollama API 与默认选项
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(baseUrl)
                    .build();
            OllamaChatOptions options = OllamaChatOptions.builder()
                    .model(modelName)
                    .build();

            return OllamaChatModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(options)
                    .build();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("创建 Ollama 模型失败: {}", errorMessage, e);
            throw new RuntimeException("创建 Ollama 模型失败", e);
        }
    }

    /**
     * 对外暴露 getModelType 作为调用入口，便于上层复用。
     *
     * 为什么：工厂需要根据类型路由 Provider
     * 入参：无
     * 出参：模型类型
     */
    @Override
    public ModelType getModelType() {
        return ModelType.OLLAMA;
    }

    /**
     * 对外暴露 isHealthy 作为调用入口，便于上层复用。
     *
     * 为什么：快速验证模型配置可用性
     * 入参：模型配置
     * 出参：是否健康
     */
    @Override
    public boolean isHealthy(ModelConfig config) {
        try {
            createChatModel(config);
            return true;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.warn("Ollama 模型健康检查失败: {}", errorMessage);
            return false;
        }
    }
}
