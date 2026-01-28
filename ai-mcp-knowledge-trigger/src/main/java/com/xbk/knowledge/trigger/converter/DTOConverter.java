package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
// API 层 DTO 导入
import com.xbk.knowledge.api.dto.AIRequest;
import com.xbk.knowledge.api.dto.AIResponse;
import com.xbk.knowledge.api.dto.CallMetricsDTO;
import com.xbk.knowledge.api.dto.ModelCapabilityDTO;
import com.xbk.knowledge.api.dto.ModelInfo;
import com.xbk.knowledge.api.dto.ModelUsageDTO;
import com.xbk.knowledge.api.dto.ResponseTimeDTO;
import com.xbk.knowledge.api.dto.SuccessRateDTO;
// Domain 层 DTO 导入
import com.xbk.knowledge.domain.model.dto.DomainAIRequest;
import com.xbk.knowledge.domain.model.dto.DomainAIResponse;
import com.xbk.knowledge.domain.model.dto.DomainCallMetricsDTO;
import com.xbk.knowledge.domain.model.dto.DomainModelInfo;
import com.xbk.knowledge.domain.model.dto.DomainModelUsageDTO;
import com.xbk.knowledge.domain.model.dto.DomainResponseTimeDTO;
import com.xbk.knowledge.domain.model.dto.DomainSuccessRateDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO 转换工具类
 * 负责 Domain DTO 与 API DTO 之间的转换（接口适配层职责）
 *
 * @author xiexu
 */
public class DTOConverter {

    /**
     * Domain AIRequest -> API AIRequest
     */
    public static AIRequest toApiAIRequest(DomainAIRequest domain) {
        if (domain == null) {
            return null;
        }
        return AIRequest.builder()
                .content(domain.getContent())
                .taskType(domain.getTaskType())
                .systemPrompt(domain.getSystemPrompt())
                .parameters(domain.getParameters())
                .strategy(domain.getStrategy())
                .streaming(domain.getStreaming())
                .build();
    }

    /**
     * API AIRequest -> Domain AIRequest
     */
    public static DomainAIRequest toDomainAIRequest(AIRequest api) {
        if (api == null) {
            return null;
        }
        return DomainAIRequest.builder()
                .content(api.getContent())
                .taskType(api.getTaskType())
                .systemPrompt(api.getSystemPrompt())
                .parameters(api.getParameters())
                .strategy(api.getStrategy())
                .streaming(api.getStreaming())
                .build();
    }

    /**
     * Domain AIResponse -> API AIResponse
     */
    public static AIResponse toApiAIResponse(DomainAIResponse domain) {
        if (domain == null) {
            return null;
        }
        return AIResponse.builder()
                .content(domain.getContent())
                .modelUsed(domain.getModelUsed())
                .tokensUsed(domain.getTokensUsed())
                .responseTime(domain.getResponseTime())
                .success(domain.getSuccess())
                .errorMessage(domain.getErrorMessage())
                .fallback(domain.getFallback())
                .retryCount(domain.getRetryCount())
                .build();
    }

    /**
     * Domain ModelInfo -> API ModelInfo
     */
    public static ModelInfo toApiModelInfo(DomainModelInfo domain) {
        if (domain == null) {
            return null;
        }
        return ModelInfo.builder()
                .modelId(domain.getModelId())
                .modelName(domain.getModelName())
                .modelType(domain.getModelType())
                .qualityScore(domain.getQualityScore())
                .enabled(domain.getEnabled())
                .capability(toApiModelCapability(domain.getCapability()))
                .build();
    }

    /**
     * Domain ModelInfo List -> API ModelInfo List
     */
    public static List<ModelInfo> toApiModelInfoList(List<DomainModelInfo> domainList) {
        if (domainList == null) {
            return null;
        }
        return domainList.stream()
                .map(DTOConverter::toApiModelInfo)
                .collect(Collectors.toList());
    }

    /**
     * Domain ModelCapability -> API ModelCapabilityDTO
     */
    public static ModelCapabilityDTO toApiModelCapability(ModelCapability domain) {
        if (domain == null) {
            return null;
        }
        return ModelCapabilityDTO.builder()
                .maxInputTokens(domain.getMaxInputTokens())
                .maxOutputTokens(domain.getMaxOutputTokens())
                .supportFunctionCalling(domain.getSupportFunctionCalling())
                .supportVision(domain.getSupportVision())
                .supportStreaming(domain.getSupportStreaming())
                .qualityScore(domain.getQualityScore())
                .build();
    }

    /**
     * API ModelCapabilityDTO -> Domain ModelCapability Entity
     */
    public static ModelCapability toDomainModelCapability(ModelCapabilityDTO api) {
        if (api == null) {
            return null;
        }
        return ModelCapability.builder()
                .maxInputTokens(api.getMaxInputTokens())
                .maxOutputTokens(api.getMaxOutputTokens())
                .supportFunctionCalling(api.getSupportFunctionCalling())
                .supportVision(api.getSupportVision())
                .supportStreaming(api.getSupportStreaming())
                .qualityScore(api.getQualityScore())
                .build();
    }

    /**
     * Domain CallMetricsDTO -> API CallMetricsDTO
     */
    public static CallMetricsDTO toApiCallMetrics(DomainCallMetricsDTO domain) {
        if (domain == null) {
            return null;
        }
        return CallMetricsDTO.builder()
                .totalCalls(domain.getTotalCalls())
                .successCalls(domain.getSuccessCalls())
                .failedCalls(domain.getFailedCalls())
                .fallbackCalls(domain.getFallbackCalls())
                .build();
    }

    /**
     * Domain SuccessRateDTO -> API SuccessRateDTO
     */
    public static SuccessRateDTO toApiSuccessRate(DomainSuccessRateDTO domain) {
        if (domain == null) {
            return null;
        }
        return SuccessRateDTO.builder()
                .totalCalls(domain.getTotalCalls())
                .successCalls(domain.getSuccessCalls())
                .successRate(domain.getSuccessRate())
                .build();
    }

    /**
     * Domain ResponseTimeDTO -> API ResponseTimeDTO
     */
    public static ResponseTimeDTO toApiResponseTime(DomainResponseTimeDTO domain) {
        if (domain == null) {
            return null;
        }
        return ResponseTimeDTO.builder()
                .avgResponseTime(domain.getAvgResponseTime())
                .maxResponseTime(domain.getMaxResponseTime())
                .minResponseTime(domain.getMinResponseTime())
                .build();
    }

    /**
     * Domain ModelUsageDTO -> API ModelUsageDTO
     */
    public static ModelUsageDTO toApiModelUsage(DomainModelUsageDTO domain) {
        if (domain == null) {
            return null;
        }
        return ModelUsageDTO.builder()
                .modelId(domain.getModelId())
                .callCount(domain.getCallCount())
                .usageRate(domain.getUsageRate())
                .build();
    }

    /**
     * Domain ModelUsageDTO List -> API ModelUsageDTO List
     */
    public static List<ModelUsageDTO> toApiModelUsageList(List<DomainModelUsageDTO> domainList) {
        if (domainList == null) {
            return null;
        }
        return domainList.stream()
                .map(DTOConverter::toApiModelUsage)
                .collect(Collectors.toList());
    }
}
