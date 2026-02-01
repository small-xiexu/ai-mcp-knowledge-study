package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.model.ModelCapabilityDTO;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;

import java.util.List;
import java.util.Map;

/**
 * DTO 转换工具类
 * 负责应用层与 API DTO 的转换（接口适配层职责）
 *
 * 职责：接口层 DTO 转换，用于隔离传输与领域模型
 * @author xiexu
 */
public class DTOConverter {

    /**
     * API AIRequest -> 应用层 AICallCommand
     */
    public static AICallCommand toAppAICallCommand(AIRequest api) {
        if (api == null) {
            return null;
        }
        // 只做字段映射，不引入业务逻辑，保持接口层与应用层解耦
        String content = api.getContent();
        String taskType = api.getTaskType();
        String systemPrompt = api.getSystemPrompt();
        Map<String, Object> parameters = api.getParameters();
        ModelSelectionStrategy strategy = api.getStrategy();
        Boolean streaming = api.getStreaming();
        Long modelId = api.getModelId();
        List<String> ragTags = api.getRagTags();
        return AICallCommand.builder()
                .content(content)
                .taskType(taskType)
                .systemPrompt(systemPrompt)
                .parameters(parameters)
                .strategy(strategy)
                .streaming(streaming)
                .modelId(modelId)
                .ragTags(ragTags)
                .build();
    }

    /**
     * 应用层 AICallResult -> API AIResponse
     */
    public static AIResponse toApiAIResponse(AICallResult result) {
        if (result == null) {
            return null;
        }
        String content = result.getContent();
        String modelUsed = result.getModelUsed();
        Integer tokensUsed = result.getTokensUsed();
        Long responseTime = result.getResponseTime();
        Boolean success = result.getSuccess();
        String errorMessage = result.getErrorMessage();
        Boolean fallback = result.getFallback();
        Integer retryCount = result.getRetryCount();
        return AIResponse.builder()
                .content(content)
                .modelUsed(modelUsed)
                .tokensUsed(tokensUsed)
                .responseTime(responseTime)
                .success(success)
                .errorMessage(errorMessage)
                .fallback(fallback)
                .retryCount(retryCount)
                .build();
    }

    /**
     * ModelCapability -> ModelCapabilityDTO
     */
    public static ModelCapabilityDTO toApiModelCapability(ModelCapability capability) {
        if (capability == null) {
            return null;
        }
        Integer qualityScore = capability.getQualityScore();
        Integer maxInputTokens = capability.getMaxInputTokens();
        Integer maxOutputTokens = capability.getMaxOutputTokens();
        Boolean supportStreaming = capability.getSupportStreaming();
        Boolean supportFunctionCalling = capability.getSupportFunctionCalling();
        Boolean supportVision = capability.getSupportVision();
        return ModelCapabilityDTO.builder()
                .qualityScore(qualityScore)
                .maxInputTokens(maxInputTokens)
                .maxOutputTokens(maxOutputTokens)
                .supportStreaming(supportStreaming)
                .supportFunctionCalling(supportFunctionCalling)
                .supportVision(supportVision)
                .build();
    }
}
