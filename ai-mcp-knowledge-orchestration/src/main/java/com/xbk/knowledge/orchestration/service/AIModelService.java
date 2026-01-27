package com.xbk.knowledge.orchestration.service;

import com.xbk.knowledge.orchestration.model.dto.AIRequest;
import com.xbk.knowledge.orchestration.model.dto.AIResponse;
import com.xbk.knowledge.orchestration.model.dto.ModelInfo;

import java.util.List;

/**
 * AI 模型统一服务接口
 * 提供统一的 AI 模型调用入口
 *
 * @author xiexu
 */
public interface AIModelService {

    /**
     * 通用聊天接口
     * 根据请求中的策略自动选择模型
     *
     * @param request AI 请求对象
     * @return AI 响应对象
     */
    AIResponse chat(AIRequest request);

    /**
     * 根据任务类型聊天
     * 自动选择该任务类型的首选模型，失败时尝试备用模型
     *
     * @param taskType 任务类型编码
     * @param request  AI 请求对象
     * @return AI 响应对象
     */
    AIResponse chatByTaskType(String taskType, AIRequest request);

    /**
     * 获取所有可用模型列表
     * 返回所有启用状态的模型信息
     *
     * @return 可用模型列表
     */
    List<ModelInfo> getAvailableModels();

    /**
     * 获取指定任务类型的推荐模型
     * 根据任务类型返回最适合的模型信息
     *
     * @param taskType 任务类型编码
     * @return 推荐的模型信息
     */
    ModelInfo getRecommendedModel(String taskType);
}
