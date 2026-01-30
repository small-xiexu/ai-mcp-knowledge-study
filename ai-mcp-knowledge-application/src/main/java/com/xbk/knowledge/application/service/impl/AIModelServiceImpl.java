package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.application.fallback.FallbackHandler;
import com.xbk.knowledge.application.fallback.ModelCallContext;
import com.xbk.knowledge.application.fallback.ModelCallExecutor;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import com.xbk.knowledge.application.service.AIModelService;
import com.xbk.knowledge.application.service.ModelSelector;
import com.xbk.knowledge.application.service.selection.ModelSelectionChain;
import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import com.xbk.knowledge.types.enums.CallStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 模型统一服务实现类
 * 提供统一的 AI 模型调用入口实现
 *
 * 职责：应用层用例实现，用于编排领域与基础设施
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelServiceImpl implements AIModelService {

    private final ModelSelector modelSelector;
    private final ModelSelectionChain modelSelectionChain;
    private final ModelCallExecutor modelCallExecutor;
    private final CallLogRepository callLogRepository;
    private final FallbackHandler fallbackHandler;

    /**
     * 统一聊天入口
     * 通过选择链路隔离策略差异，保证调用入口稳定一致
     *
     * @param request 请求对象
     * @return 调用结果
     */
    @Override
    public AICallResult chat(AICallCommand request) {
        String content = request.getContent();
        log.info("开始处理 chat 请求，content: {}", content);

        // 选择链优先级：显式策略 > 任务类型 > 默认策略
        ModelConfig selectedModel;
        ModelSelectionDecision decision = modelSelectionChain.select(request);
        if (decision.isUseTaskType()) {
            // 任务类型有明确的业务语义，优先使用任务配置的模型
            request.setTaskType(decision.getTaskType());
            return chatByTaskType(request);
        }
        selectedModel = decision.getSelectedModel();

        // 执行调用
        return executeCall(selectedModel, request, false);
    }

    /**
     * 按任务类型调用
     * 保证任务语义优先并统一降级策略的处理路径
     *
     * @param request 请求对象
     * @return 调用结果
     */
    @Override
    public AICallResult chatByTaskType(AICallCommand request) {
        String content = request.getContent();
        String taskType = request.getTaskType();
        log.info("开始处理 chatByTaskType 请求，taskType: {}, content: {}", taskType, content);

        // 根据任务类型选择模型
        ModelSelectionResult selectionResult = modelSelector.selectModel(taskType);
        ModelConfig primaryModel = selectionResult.getPrimaryModel();
        List<ModelConfig> fallbackModels = selectionResult.getFallbackModels();

        // 使用 FallbackHandler 执行带降级的调用
        AICallResult response = fallbackHandler.executeWithFallback(primaryModel, fallbackModels, request);

        // 记录调用日志
        recordCallLog(primaryModel, request, response);

        return response;
    }

    /**
     * 执行模型调用
     * 核心调用逻辑，包含异常处理和日志记录
     *
     * @param modelConfig 模型配置
     * @param request     请求对象
     * @param isFallback  是否为降级调用
     * @return 响应对象
     */
    private AICallResult executeCall(ModelConfig modelConfig, AICallCommand request, boolean isFallback) {
        Long modelId = modelConfig.getId();
        String taskType = request.getTaskType();
        String requestContent = request.getContent();
        String truncatedRequestContent = truncateContent(requestContent, 5000);
        CallLog callLog = CallLog.builder()
                .modelId(modelId)
                .taskType(taskType)
                .requestContent(truncatedRequestContent)
                .build();

        ModelCallContext context = ModelCallContext.builder()
                .model(modelConfig)
                .request(request)
                .build();
        AICallResult response = modelCallExecutor.execute(context);
        if (response == null) {
            response = AICallResult.builder()
                    .success(false)
                    .errorMessage("模型调用失败")
                    .modelUsed(modelConfig.getModelName())
                    .fallback(isFallback)
                    .build();
        }

        Long responseTime = response.getResponseTime();
        Long safeResponseTime = responseTime != null ? responseTime : 0L;
        Integer tokensUsed = response.getTokensUsed();
        Integer safeTokensUsed = tokensUsed != null ? tokensUsed : 0;
        Boolean success = response.getSuccess();

        if (Boolean.TRUE.equals(success)) {
            // 记录成功日志
            String responseContent = response.getContent();
            String truncateContent = truncateContent(responseContent, 5000);
            callLog.setResponseContent(truncateContent);
            callLog.setTokensUsed(safeTokensUsed);
            callLog.setResponseTime(safeResponseTime);
            CallStatus callStatus = isFallback ? CallStatus.FALLBACK : CallStatus.SUCCESS;
            callLog.setStatus(callStatus);
            LocalDateTime createdAt = LocalDateTime.now();
            callLog.setCreatedAt(createdAt);
            CallLogAggregate aggregate = CallLogAggregate.builder()
                    .callLog(callLog)
                    .build();
            callLogRepository.save(aggregate);

            log.info("模型调用成功，modelId: {}, responseTime: {}ms", modelId, safeResponseTime);
            response.setFallback(isFallback);
            return response;
        }

        // 记录失败日志
        callLog.setResponseContent(null);
        callLog.setTokensUsed(safeTokensUsed);
        callLog.setResponseTime(safeResponseTime);
        callLog.setStatus(CallStatus.FAILED);
        String errorMessage = response.getErrorMessage();
        callLog.setErrorMessage(errorMessage);
        LocalDateTime createdAt = LocalDateTime.now();
        callLog.setCreatedAt(createdAt);
        CallLogAggregate aggregate = CallLogAggregate.builder()
                .callLog(callLog)
                .build();
        callLogRepository.save(aggregate);

        log.error("模型调用失败，modelId: {}, error: {}", modelId, errorMessage);
        response.setFallback(isFallback);
        return response;
    }

    /**
     * 截断内容
     * 避免日志内容过长
     *
     * @param content   原始内容
     * @param maxLength 最大长度
     * @return 截断后的内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 记录调用日志
     *
     * @param modelConfig 模型配置
     * @param request     请求对象
     * @param response    响应对象
     */
    private void recordCallLog(ModelConfig modelConfig, AICallCommand request, AICallResult response) {
        Long modelId = modelConfig.getId();
        String taskType = request.getTaskType();
        String requestContent = request.getContent();
        String responseContent = response.getContent();
        Integer tokensUsed = response.getTokensUsed();
        Long responseTime = response.getResponseTime();
        Boolean success = response.getSuccess();
        Boolean fallback = response.getFallback();
        String errorMessage = response.getErrorMessage();
        String truncatedRequestContent = truncateContent(requestContent, 5000);
        String truncatedResponseContent = truncateContent(responseContent, 5000);
        Integer safeTokensUsed = tokensUsed != null ? tokensUsed : 0;
        Long safeResponseTime = responseTime != null ? responseTime : 0L;
        CallStatus status = Boolean.TRUE.equals(success)
                ? (Boolean.TRUE.equals(fallback) ? CallStatus.FALLBACK : CallStatus.SUCCESS)
                : CallStatus.FAILED;
        LocalDateTime createdAt = LocalDateTime.now();
        CallLog callLog = CallLog.builder()
                .modelId(modelId)
                .taskType(taskType)
                .requestContent(truncatedRequestContent)
                .responseContent(truncatedResponseContent)
                .tokensUsed(safeTokensUsed)
                .responseTime(safeResponseTime)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(createdAt)
                .build();

        CallLogAggregate aggregate = CallLogAggregate.builder()
                .callLog(callLog)
                .build();
        callLogRepository.save(aggregate);
    }
}
