package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.model.ModelCapabilityDTO;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.model.entity.ModelCapability;

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
        return AICallCommand.builder()
                .content(api.getContent())
                .taskType(api.getTaskType())
                .systemPrompt(api.getSystemPrompt())
                .parameters(api.getParameters())
                .strategy(api.getStrategy())
                .streaming(api.getStreaming())
                .build();
    }

    /**
     * 应用层 AICallResult -> API AIResponse
     */
    public static AIResponse toApiAIResponse(AICallResult result) {
        if (result == null) {
            return null;
        }
        return AIResponse.builder()
                .content(result.getContent())
                .modelUsed(result.getModelUsed())
                .tokensUsed(result.getTokensUsed())
                .responseTime(result.getResponseTime())
                .success(result.getSuccess())
                .errorMessage(result.getErrorMessage())
                .fallback(result.getFallback())
                .retryCount(result.getRetryCount())
                .build();
    }

    /**
     * ModelCapability -> ModelCapabilityDTO
     */
    public static ModelCapabilityDTO toApiModelCapability(ModelCapability capability) {
        if (capability == null) {
            return null;
        }
        return ModelCapabilityDTO.builder()
                .qualityScore(capability.getQualityScore())
                .maxInputTokens(capability.getMaxInputTokens())
                .maxOutputTokens(capability.getMaxOutputTokens())
                .supportStreaming(capability.getSupportStreaming())
                .supportFunctionCalling(capability.getSupportFunctionCalling())
                .supportVision(capability.getSupportVision())
                .build();
    }
}
