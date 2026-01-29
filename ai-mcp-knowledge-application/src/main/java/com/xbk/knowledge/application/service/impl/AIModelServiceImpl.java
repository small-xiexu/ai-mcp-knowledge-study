package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.application.fallback.FallbackHandler;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.AIModelService;
import com.xbk.knowledge.application.service.ModelSelector;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import com.xbk.knowledge.types.enums.CallStatus;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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
    private final ModelProviderFactory providerFactory;
    private final CallLogRepository callLogRepository;
    private final FallbackHandler fallbackHandler;

    @Override
    public AICallResult chat(AICallCommand request) {
        log.info("开始处理 chat 请求，content: {}", request.getContent());

        // 模型选择优先级：显式策略 > 任务类型 > 默认策略
        // 当前仅实现质量优先策略，其它策略预留扩展
        ModelConfig selectedModel;
        if (request.getStrategy() == ModelSelectionStrategy.QUALITY_PRIORITY) {
            selectedModel = modelSelector.selectByQualityPriority();
        } else if (request.getTaskType() != null) {
            // 任务类型有明确的业务语义，优先使用任务配置的模型
            return chatByTaskType(request.getTaskType(), request);
        } else {
            // 默认使用质量优先策略
            selectedModel = modelSelector.selectByQualityPriority();
        }

        // 执行调用
        return executeCall(selectedModel, request, false);
    }

    @Override
    public AICallResult chatByTaskType(String taskType, AICallCommand request) {
        log.info("开始处理 chatByTaskType 请求，taskType: {}, content: {}", taskType, request.getContent());

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
        long startTime = System.currentTimeMillis();
        CallLog callLog = CallLog.builder()
                .modelId(modelConfig.getId())
                .taskType(request.getTaskType())
                .requestContent(truncateContent(request.getContent(), 5000))
                .build();

        try {
            // 通过 ProviderFactory 统一创建 ChatClient，隔离不同厂商差异
            ChatClient chatClient = providerFactory.createChatClient(modelConfig);

            // 统一拼接系统提示词，避免业务层自行处理拼接逻辑
            String promptText = request.getSystemPrompt() != null
                    ? request.getSystemPrompt() + "\n\n" + request.getContent()
                    : request.getContent();

            // 执行调用
            String responseContent = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            long responseTime = System.currentTimeMillis() - startTime;

            // 记录成功日志
            callLog.setResponseContent(truncateContent(responseContent, 5000));
            callLog.setTokensUsed(0); // Spring AI 可能无法提供，设为 0
            callLog.setResponseTime(responseTime);
            callLog.setStatus(isFallback ? CallStatus.FALLBACK : CallStatus.SUCCESS);
            callLog.setCreatedAt(LocalDateTime.now());
            callLogRepository.save(callLog);

            log.info("模型调用成功，modelId: {}, responseTime: {}ms", modelConfig.getId(), responseTime);

            return AICallResult.builder()
                    .content(responseContent)
                    .modelUsed(modelConfig.getModelName())
                    .tokensUsed(0)
                    .responseTime(responseTime)
                    .success(true)
                    .fallback(isFallback)
                    .retryCount(0)
                    .build();

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // 记录失败日志
            callLog.setResponseContent(null);
            callLog.setTokensUsed(0);
            callLog.setResponseTime(responseTime);
            callLog.setStatus(CallStatus.FAILED);
            callLog.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            callLog.setCreatedAt(LocalDateTime.now());
            callLogRepository.save(callLog);

            log.error("模型调用失败，modelId: {}, error: {}", modelConfig.getId(), e.getMessage(), e);

            return AICallResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage() != null ? e.getMessage() : "调用失败：" + e.getClass().getSimpleName())
                    .modelUsed(modelConfig.getModelName())
                    .responseTime(responseTime)
                    .fallback(isFallback)
                    .build();
        }
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
        CallLog callLog = CallLog.builder()
                .modelId(modelConfig.getId())
                .taskType(request.getTaskType())
                .requestContent(truncateContent(request.getContent(), 5000))
                .responseContent(truncateContent(response.getContent(), 5000))
                .tokensUsed(response.getTokensUsed() != null ? response.getTokensUsed() : 0)
                .responseTime(response.getResponseTime() != null ? response.getResponseTime() : 0L)
                .status(response.getSuccess() ?
                        (response.getFallback() ? CallStatus.FALLBACK : CallStatus.SUCCESS)
                        : CallStatus.FAILED)
                .errorMessage(response.getErrorMessage())
                .createdAt(LocalDateTime.now())
                .build();

        callLogRepository.save(callLog);
    }
}
