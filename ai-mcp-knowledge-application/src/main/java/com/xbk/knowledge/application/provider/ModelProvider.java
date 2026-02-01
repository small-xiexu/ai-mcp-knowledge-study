package com.xbk.knowledge.application.provider;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 模型提供者接口
 * 定义了创建和管理 AI 模型的契约
 *
 * 职责：模型调用抽象契约，用于隔离厂商差异
 * @author xiexu
 */
public interface ModelProvider {

    /**
     * 创建 ChatClient
     *
     * @param config 模型配置
     * @return ChatClient 实例
     */
    ChatClient createChatClient(ModelConfig config);

    /**
     * 创建 ChatModel（用于流式调用）
     *
     * @param config 模型配置
     * @return ChatModel 实例
     */
    ChatModel createChatModel(ModelConfig config);

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
