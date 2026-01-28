package com.xbk.knowledge.orchestration.provider;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ModelType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 模型提供者接口
 * 定义统一的模型调用入口
 *
 * @author xiexu
 */
public interface ModelProvider {

    /**
     * 创建聊天模型
     *
     * @param config 模型配置
     * @return ChatModel 实例
     */
    ChatModel createChatModel(ModelConfig config);

    /**
     * 创建聊天客户端
     * 为保持向后兼容保留该方法，但不包含统一增强逻辑
     * 推荐通过 ModelProviderFactory#createChatClient 获取增强后的客户端
     *
     * @param config 模型配置
     * @return ChatClient 实例
     */
    @Deprecated(since = "2026-01-28", forRemoval = false)
    default ChatClient createChatClient(ModelConfig config) {
        var chatModel = createChatModel(config);
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 获取模型类型
     *
     * @return 模型类型
     */
    ModelType getModelType();

    /**
     * 检查模型健康状态
     *
     * @param config 模型配置
     * @return 是否健康
     */
    boolean isHealthy(ModelConfig config);
}
