package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;

import java.util.List;
import java.util.Map;

/**
 * DTO 转换工具类
 * 负责应用层与 API DTO 的转换（接口适配层职责）
 *
 * 职责：接口层 DTO 转换，用于隔离传输与领域模型
 *
 * @author sxie
 */
public class DTOConverter {

    /**
     * API AIRequest -> 应用层 AICallCommand
     *
     * 为什么：接口层只做字段映射，避免引入业务逻辑导致边界污染。
     */
    public static AICallCommand toAppAICallCommand(AIRequest api) {
        if (api == null) {
            return null;
        }
        String content = api.getContent();
        String systemPrompt = api.getSystemPrompt();
        Map<String, Object> parameters = api.getParameters();
        Boolean streaming = api.getStreaming();
        Long modelId = api.getModelId();
        Long sessionId = api.getSessionId();
        List<String> ragTags = api.getRagTags();
        return AICallCommand.builder()
                .content(content)
                .systemPrompt(systemPrompt)
                .parameters(parameters)
                .streaming(streaming)
                .modelId(modelId)
                .sessionId(sessionId)
                .ragTags(ragTags)
                .build();
    }

    /**
     * 应用层 AICallResult -> API AIResponse
     *
     * 为什么：统一响应字段映射，保证对外返回结构稳定。
     */
    public static AIResponse toApiAIResponse(AICallResult result) {
        if (result == null) {
            return null;
        }
        String content = result.getContent();
        String modelUsed = result.getModelUsed();
        Long responseTime = result.getResponseTime();
        Boolean success = result.getSuccess();
        String errorMessage = result.getErrorMessage();
        Boolean fallback = result.getFallback();
        Integer retryCount = result.getRetryCount();
        return AIResponse.builder()
                .content(content)
                .modelUsed(modelUsed)
                .responseTime(responseTime)
                .success(success)
                .errorMessage(errorMessage)
                .fallback(fallback)
                .retryCount(retryCount)
                .build();
    }
}
