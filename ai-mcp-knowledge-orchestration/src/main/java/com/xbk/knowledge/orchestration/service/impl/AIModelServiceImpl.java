package com.xbk.knowledge.orchestration.service.impl;

import com.xbk.knowledge.orchestration.domain.entity.CallLog;
import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.domain.repository.CallLogRepository;
import com.xbk.knowledge.orchestration.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.orchestration.model.dto.AIRequest;
import com.xbk.knowledge.orchestration.model.dto.AIResponse;
import com.xbk.knowledge.orchestration.model.dto.ModelInfo;
import com.xbk.knowledge.orchestration.model.dto.ModelSelectionResult;
import com.xbk.knowledge.orchestration.model.enums.CallStatus;
import com.xbk.knowledge.orchestration.model.enums.ModelSelectionStrategy;
import com.xbk.knowledge.orchestration.provider.ModelProviderFactory;
import com.xbk.knowledge.orchestration.service.AIModelService;
import com.xbk.knowledge.orchestration.service.ModelSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型统一服务实现类
 * 提供统一的 AI 模型调用入口实现
 *
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelServiceImpl implements AIModelService {

    private final ModelSelector modelSelector;
    private final ModelProviderFactory providerFactory;
    private final CallLogRepository callLogRepository;
    private final ModelConfigRepository modelConfigRepository;

    @Override
    public AIResponse chat(AIRequest request) {
        log.info("开始处理 chat 请求，content: {}", request.getContent());

        // 根据策略选择模型
        ModelConfig selectedModel;
        if (request.getStrategy() == ModelSelectionStrategy.QUALITY_PRIORITY) {
            selectedModel = modelSelector.selectByQualityPriority();
        } else if (request.getTaskType() != null) {
            // 如果指定了任务类型，使用任务类型选择
            return chatByTaskType(request.getTaskType(), request);
        } else {
            // 默认使用质量优先策略
            selectedModel = modelSelector.selectByQualityPriority();
        }

        // 执行调用
        return executeCall(selectedModel, request, false);
    }

    @Override
    public AIResponse chatByTaskType(String taskType, AIRequest request) {
        log.info("开始处理 chatByTaskType 请求，taskType: {}, content: {}", taskType, request.getContent());

        // 根据任务类型选择模型
        ModelSelectionResult selectionResult = modelSelector.selectModel(taskType);
        ModelConfig primaryModel = selectionResult.getPrimaryModel();
        List<ModelConfig> fallbackModels = selectionResult.getFallbackModels();

        // 尝试使用首选模型
        try {
            AIResponse response = executeCall(primaryModel, request, false);
            if (response.getSuccess()) {
                return response;
            }
        } catch (Exception e) {
            log.error("首选模型调用失败，modelId: {}, error: {}", primaryModel.getId(), e.getMessage());
        }

        // 首选模型失败，尝试备用模型
        for (ModelConfig fallbackModel : fallbackModels) {
            try {
                log.info("尝试使用备用模型，modelId: {}", fallbackModel.getId());
                AIResponse response = executeCall(fallbackModel, request, true);
                if (response.getSuccess()) {
                    return response;
                }
            } catch (Exception e) {
                log.error("备用模型调用失败，modelId: {}, error: {}", fallbackModel.getId(), e.getMessage());
            }
        }

        // 所有模型都失败
        return AIResponse.builder()
                .success(false)
                .errorMessage("所有模型调用均失败")
                .fallback(true)
                .build();
    }

    @Override
    public List<ModelInfo> getAvailableModels() {
        log.info("获取所有可用模型列表");

        // 查询所有启用的模型
        List<ModelConfig> enabledModels = modelConfigRepository.findByEnabledTrue();

        // 转换为 ModelInfo
        return enabledModels.stream()
                .map(this::convertToModelInfo)
                .collect(Collectors.toList());
    }

    @Override
    public ModelInfo getRecommendedModel(String taskType) {
        log.info("获取推荐模型，taskType: {}", taskType);

        // 根据任务类型选择模型
        ModelSelectionResult selectionResult = modelSelector.selectModel(taskType);
        ModelConfig primaryModel = selectionResult.getPrimaryModel();

        return convertToModelInfo(primaryModel);
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
    private AIResponse executeCall(ModelConfig modelConfig, AIRequest request, boolean isFallback) {
        long startTime = System.currentTimeMillis();
        CallLog callLog = CallLog.builder()
                .modelId(modelConfig.getId())
                .taskType(request.getTaskType())
                .requestContent(truncateContent(request.getContent(), 5000))
                .build();

        try {
            // 创建 ChatClient
            ChatClient chatClient = providerFactory.createChatClient(modelConfig);

            // 构建 Prompt
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

            return AIResponse.builder()
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

            return AIResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage() != null ? e.getMessage() : "调用失败：" + e.getClass().getSimpleName())
                    .modelUsed(modelConfig.getModelName())
                    .responseTime(responseTime)
                    .fallback(isFallback)
                    .build();
        }
    }

    /**
     * 转换为 ModelInfo
     *
     * @param modelConfig 模型配置
     * @return ModelInfo
     */
    private ModelInfo convertToModelInfo(ModelConfig modelConfig) {
        return ModelInfo.builder()
                .modelId(modelConfig.getId())
                .modelName(modelConfig.getModelName())
                .modelType(modelConfig.getModelType())
                .enabled(modelConfig.getEnabled())
                .qualityScore(modelConfig.getCapability() != null
                        ? modelConfig.getCapability().getQualityScore()
                        : null)
                .capability(modelConfig.getCapability())
                .build();
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
}
