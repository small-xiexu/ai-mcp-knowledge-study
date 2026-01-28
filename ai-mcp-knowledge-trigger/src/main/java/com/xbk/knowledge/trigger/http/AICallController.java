package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.IAICallService;
import com.xbk.knowledge.api.dto.AIRequest;
import com.xbk.knowledge.api.dto.AIResponse;
import com.xbk.knowledge.api.dto.ModelInfo;
import com.xbk.knowledge.domain.model.dto.DomainAIRequest;
import com.xbk.knowledge.domain.model.dto.DomainAIResponse;
import com.xbk.knowledge.domain.model.dto.DomainModelInfo;
import com.xbk.knowledge.domain.service.AIModelService;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * AI 调用 Controller
 * 提供统一的 AI 模型调用接口
 *
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AICallController implements IAICallService {

    private final AIModelService aiModelService;

    /**
     * 通用 AI 调用接口
     * 根据策略自动选择最优模型
     *
     * @param request AI 请求
     * @return AI 响应
     */
    @Override
    @PostMapping("/chat")
    public Result<AIResponse> chat(@Valid @RequestBody AIRequest request) {
        log.info("收到 AI 调用请求，content length: {}, strategy: {}",
                request.getContent() != null ? request.getContent().length() : 0,
                request.getStrategy());

        // API DTO → Domain DTO
        DomainAIRequest domainRequest = DTOConverter.toDomainAIRequest(request);

        // 调用 AI 服务
        DomainAIResponse domainResponse = aiModelService.chat(domainRequest);

        // Domain DTO → API DTO
        AIResponse response = DTOConverter.toApiAIResponse(domainResponse);

        // 返回结果
        if (response.getSuccess()) {
            log.info("AI 调用成功，modelUsed: {}, responseTime: {}ms",
                    response.getModelUsed(), response.getResponseTime());
            return Result.success(response);
        } else {
            log.error("AI 调用失败，error: {}", response.getErrorMessage());
            return Result.error(500, "AI 调用失败：" + response.getErrorMessage(), response);
        }
    }

    /**
     * 按任务类型调用 AI
     * 根据任务类型选择对应的模型
     *
     * @param taskType 任务类型编码
     * @param request  AI 请求
     * @return AI 响应
     */
    @Override
    @PostMapping("/chat/by-task/{taskType}")
    public Result<AIResponse> chatByTaskType(@PathVariable String taskType,
                                               @Valid @RequestBody AIRequest request) {
        log.info("收到按任务类型调用请求，taskType: {}, content length: {}",
                taskType, request.getContent() != null ? request.getContent().length() : 0);

        // 设置任务类型并转换为 Domain DTO
        request.setTaskType(taskType);
        DomainAIRequest domainRequest = DTOConverter.toDomainAIRequest(request);

        // 调用 AI 服务
        DomainAIResponse domainResponse = aiModelService.chatByTaskType(taskType, domainRequest);

        // Domain DTO → API DTO
        AIResponse response = DTOConverter.toApiAIResponse(domainResponse);

        // 返回结果
        if (response.getSuccess()) {
            log.info("AI 调用成功，taskType: {}, modelUsed: {}, responseTime: {}ms, fallback: {}",
                    taskType, response.getModelUsed(), response.getResponseTime(), response.getFallback());
            return Result.success(response);
        } else {
            log.error("AI 调用失败，taskType: {}, error: {}", taskType, response.getErrorMessage());
            return Result.error(500, "AI 调用失败：" + response.getErrorMessage(), response);
        }
    }

    /**
     * 获取所有可用模型列表
     *
     * @return 模型列表
     */
    @Override
    @GetMapping("/models/available")
    public Result<List<ModelInfo>> getAvailableModels() {
        log.info("查询可用模型列表");

        // 调用 Domain 服务，返回 Domain DTO List
        List<DomainModelInfo> domainModels = aiModelService.getAvailableModels();

        // Domain DTO List → API DTO List
        List<ModelInfo> models = DTOConverter.toApiModelInfoList(domainModels);

        log.info("查询到 {} 个可用模型", models.size());
        return Result.success(models);
    }

    /**
     * 获取推荐模型
     * 根据任务类型返回推荐的模型
     *
     * @param taskType 任务类型编码（可选）
     * @return 推荐模型
     */
    @Override
    @GetMapping("/models/recommended")
    public Result<ModelInfo> getRecommendedModel(@RequestParam(required = false) String taskType) {
        log.info("查询推荐模型，taskType: {}", taskType);

        // 调用 Domain 服务，返回 Domain DTO
        DomainModelInfo domainModel = aiModelService.getRecommendedModel(taskType);

        // Domain DTO → API DTO
        ModelInfo model = DTOConverter.toApiModelInfo(domainModel);

        log.info("推荐模型：{}", model.getModelName());
        return Result.success(model);
    }
}
