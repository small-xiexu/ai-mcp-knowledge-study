package com.xbk.knowledge.infrastructure.provider.gemini;

import com.xbk.knowledge.infrastructure.protocol.AbstractGeminiProtocolAdapter;
import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
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
 * @author sxie
 */
@Slf4j
@Component
public class GeminiModelProvider extends AbstractGeminiProtocolAdapter implements ModelProvider {

    /**
     * 构建 ChatModel
     *
     * 为什么：统一捕获 SDK 异常并输出可读日志
     * 入参：模型配置
     * 出参：ChatModel
     */
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        try {
            return super.createChatModel(config);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("创建 Gemini 模型失败: {}", errorMessage, e);
            throw new RuntimeException("创建 Gemini 模型失败", e);
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
        return ModelType.GEMINI;
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
            // 通过创建模型验证配置有效性
            createChatModel(config);
            return true;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.warn("Gemini 模型健康检查失败: {}", errorMessage);
            return false;
        }
    }
}
