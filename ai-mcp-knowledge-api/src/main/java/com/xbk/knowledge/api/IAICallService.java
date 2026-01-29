package com.xbk.knowledge.api;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.ai.ModelInfo;

import java.util.List;

/**
 * AI 调用服务接口
 * 定义 AI 模型调用的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author xiexu
 */
public interface IAICallService {

    /**
     * 通用 AI 调用接口
     * 根据策略自动选择最优模型
     *
     * @param request AI 请求
     * @return AI 响应
     */
    Result<AIResponse> chat(AIRequest request);

    /**
     * 按任务类型调用 AI
     * 根据任务类型选择对应的模型
     *
     * @param taskType 任务类型编码
     * @param request  AI 请求
     * @return AI 响应
     */
    Result<AIResponse> chatByTaskType(String taskType, AIRequest request);

    /**
     * 获取所有可用模型列表
     *
     * @return 模型列表
     */
    Result<List<ModelInfo>> getAvailableModels();

    /**
     * 获取推荐模型
     * 根据任务类型返回推荐的模型
     *
     * @param taskType 任务类型编码（可选）
     * @return 推荐模型
     */
    Result<ModelInfo> getRecommendedModel(String taskType);
}
