package com.xbk.knowledge.infrastructure.provider.anthropic;

import com.xbk.knowledge.infrastructure.protocol.AbstractAnthropicProtocolAdapter;
import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Anthropic 模型提供者
 * 封装 Anthropic Claude 模型的创建和调用
 *
 * 职责：模型调用实现，用于适配具体厂商 SDK
 * @author sxie
 */
@Slf4j
@Component
public class AnthropicModelProvider extends AbstractAnthropicProtocolAdapter implements ModelProvider {

    /**
     * 构建 ChatModel
     *
     * 统一捕获 SDK 异常并输出可读日志
     * 
     * @param config 配置信息。
     * @return 可用的对话模型。
     */
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        try {
            return super.createChatModel(config);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("创建 Anthropic 模型失败: {}", errorMessage, e);
            throw new RuntimeException("创建 Anthropic 模型失败", e);
        }
    }

    /**
     * 对外暴露 getModelType 作为调用入口，便于上层复用。
     *
     * 工厂需要根据类型路由 Provider
     * 
     * @return Provider 对应的模型类型。
     */
    @Override
    public ModelType getModelType() {
        return ModelType.ANTHROPIC;
    }

    /**
     * 对外暴露 isHealthy 作为调用入口，便于上层复用。
     *
     * 快速验证模型配置可用性
     * 
     * @param config 配置信息。
     * @return `true` 表示配置可用，`false` 表示配置不可用。
     */
    @Override
    public boolean isHealthy(ModelConfig config) {
        try {
            // 通过创建模型验证配置有效性
            createChatModel(config);
            return true;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.warn("Anthropic 模型健康检查失败: {}", errorMessage);
            return false;
        }
    }
}
