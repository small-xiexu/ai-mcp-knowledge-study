package com.xbk.knowledge.application.provider;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 模型提供者接口
 * 定义了创建和管理 AI 模型的契约
 *
 * 职责：模型调用抽象契约，用于隔离厂商差异
 * @author sxie
 */
public interface ModelProvider {

    /**
     * 创建 ChatModel（用于流式调用）
     *
     * 流式调用需要直接使用 ChatModel
     * 
     * @param config 配置信息。
     * @return 可用于调用的 ChatModel。
     */
    ChatModel createChatModel(ModelConfig config);

    /**
     * 获取模型类型
     *
     * 用于工厂路由 Provider
     * 
     * @return 当前 Provider 对应的模型类型。
     */
    ModelType getModelType();

    /**
     * 检查模型健康状态
     *
     * 用于预检配置有效性
     * 
     * @param config 配置信息。
     * @return `true` 表示健康检查通过，`false` 表示不可用。
     */
    boolean isHealthy(ModelConfig config);
}
