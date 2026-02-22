package com.xbk.knowledge.api;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.ai.ModelInfo;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * AI 调用服务接口
 * 定义 AI 模型调用的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
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
     * 流式 AI 调用接口。
     *
     * @param request AI 请求
     * @param httpResponse HTTP 响应
     * @return 流式响应对象
     */
    Object stream(AIRequest request, HttpServletResponse httpResponse);

    /**
     * 获取所有可用模型列表
     *
     * @return 模型列表
     */
    Result<List<ModelInfo>> getAvailableModels();

}
