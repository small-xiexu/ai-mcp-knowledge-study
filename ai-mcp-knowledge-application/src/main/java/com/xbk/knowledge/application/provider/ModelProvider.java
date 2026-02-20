package com.xbk.knowledge.application.provider;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
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
     * 创建 ChatModel（用于流式调用）
     *
     * 为什么：流式调用需要直接使用 ChatModel
     * 入参：模型配置
     * 出参：ChatModel 实例
     */
    ChatModel createChatModel(ModelConfig config);

    /**
     * 获取模型类型
     *
     * 为什么：用于工厂路由 Provider
     * 入参：无
     * 出参：模型类型
     */
    ModelType getModelType();

    /**
     * 检查模型健康状态
     *
     * 为什么：用于预检配置有效性
     * 入参：模型配置
     * 出参：是否健康
     */
    boolean isHealthy(ModelConfig config);
}
