package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.fallback.handler.FallbackHandler;
import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.executor.ModelCallExecutor;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import com.xbk.knowledge.application.service.app.AIModelService;
import com.xbk.knowledge.application.service.selector.ModelSelector;
import com.xbk.knowledge.application.service.selection.chain.ModelSelectionChain;
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
        /**
         * 组装调用日志基础信息，保证失败场景也可追溯。
         */
        CallLog callLog = buildCallLog(modelConfig, request);

        /**
         * 执行模型调用，统一通过执行器隔离模型实现细节。
         */
        ModelCallContext context = ModelCallContext.builder()
                .model(modelConfig)
                .request(request)
                .build();
        AICallResult response = modelCallExecutor.execute(context);
        if (response == null) {
            response = buildFallbackResponse(modelConfig, isFallback);
        }

        /**
         * 兜底处理响应数据，避免空值影响日志记录。
         */
        Boolean success = response.getSuccess();

        if (Boolean.TRUE.equals(success)) {
            /**
             * 成功分支：记录成功日志并返回响应。
             */
            fillSuccessLog(callLog, response, isFallback);
            saveCallLog(callLog);

            Long responseTime = resolveResponseTime(response);
            Long modelId = callLog.getModelId();
            log.info("模型调用成功，modelId: {}, responseTime: {}ms", modelId, responseTime);
            response.setFallback(isFallback);
            return response;
        }

        /**
         * 失败分支：记录失败日志并返回响应。
         */
        fillFailureLog(callLog, response);
        saveCallLog(callLog);

        Long modelId = callLog.getModelId();
        String errorMessage = response.getErrorMessage();
        log.error("模型调用失败，modelId: {}, error: {}", modelId, errorMessage);
        response.setFallback(isFallback);
        return response;
    }

    /**
     * 组装调用日志基础信息，确保请求可追溯。
     */
    private CallLog buildCallLog(ModelConfig modelConfig, AICallCommand request) {
        Long modelId = modelConfig.getId();
        String taskType = request.getTaskType();
        String requestContent = request.getContent();
        String truncatedRequestContent = truncateContent(requestContent, 5000);
        return CallLog.builder()
                .modelId(modelId)
                .taskType(taskType)
                .requestContent(truncatedRequestContent)
                .build();
    }

    /**
     * 构建兜底响应，避免执行器返回空结果。
     */
    private AICallResult buildFallbackResponse(ModelConfig modelConfig, boolean isFallback) {
        return AICallResult.builder()
                .success(false)
                .errorMessage("模型调用失败")
                .modelUsed(modelConfig.getModelName())
                .fallback(isFallback)
                .build();
    }

    /**
     * 填充成功日志字段。
     */
    private void fillSuccessLog(CallLog callLog, AICallResult response, boolean isFallback) {
        String responseContent = response.getContent();
        String truncateContent = truncateContent(responseContent, 5000);
        callLog.setResponseContent(truncateContent);
        callLog.setTokensUsed(resolveTokensUsed(response));
        callLog.setResponseTime(resolveResponseTime(response));
        CallStatus callStatus = isFallback ? CallStatus.FALLBACK : CallStatus.SUCCESS;
        callLog.setStatus(callStatus);
        callLog.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 填充失败日志字段。
     */
    private void fillFailureLog(CallLog callLog, AICallResult response) {
        callLog.setResponseContent(null);
        callLog.setTokensUsed(resolveTokensUsed(response));
        callLog.setResponseTime(resolveResponseTime(response));
        callLog.setStatus(CallStatus.FAILED);
        callLog.setErrorMessage(response.getErrorMessage());
        callLog.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 统一保存调用日志，避免重复逻辑。
     */
    private void saveCallLog(CallLog callLog) {
        CallLogAggregate aggregate = CallLogAggregate.builder()
                .callLog(callLog)
                .build();
        callLogRepository.save(aggregate);
    }

    /**
     * 响应耗时兜底处理。
     */
    private Long resolveResponseTime(AICallResult response) {
        Long responseTime = response.getResponseTime();
        return responseTime != null ? responseTime : 0L;
    }

    /**
     * token 统计兜底处理。
     */
    private Integer resolveTokensUsed(AICallResult response) {
        Integer tokensUsed = response.getTokensUsed();
        return tokensUsed != null ? tokensUsed : 0;
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
