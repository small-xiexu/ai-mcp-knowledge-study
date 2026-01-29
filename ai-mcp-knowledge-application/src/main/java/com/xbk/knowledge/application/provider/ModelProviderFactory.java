package com.xbk.knowledge.application.provider;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 模型提供者工厂接口
 * 定义了根据模型类型获取 Provider 的契约
 *
 * 职责：模型调用抽象契约，用于隔离厂商差异
 * @author xiexu
 */
public interface ModelProviderFactory {

    /**
     * 根据模型类型获取对应的 Provider
     *
     * @param modelType 模型类型
     * @return ModelProvider 实例
     * @throws IllegalArgumentException 如果模型类型不支持
     */
    ModelProvider getProvider(ModelType modelType);

    /**
     * 根据模型配置创建 ChatClient
     *
     * @param config 模型配置
     * @return ChatClient 实例
     */
    ChatClient createChatClient(ModelConfig config);

    /**
     * 检查指定模型类型是否支持
     *
     * @param modelType 模型类型
     * @return 是否支持
     */
    boolean isSupported(ModelType modelType);
}
