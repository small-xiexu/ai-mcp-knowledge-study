package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.IAICallService;
import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.ai.ModelInfo;
import com.xbk.knowledge.api.dto.ai.ModelRecommendRequest;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.service.AIModelService;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypeQuery;
import com.xbk.knowledge.application.service.ModelConfigAppService;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 调用 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AICallController implements IAICallService {

    private final AIModelService aiModelService;
    private final ModelConfigAppService modelConfigAppService;

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
        try {
            AICallCommand command = DTOConverter.toAppAICallCommand(request);
            AICallResult result = aiModelService.chat(command);
            AIResponse response = DTOConverter.toApiAIResponse(result);

            return Result.success(response);
        } catch (Exception e) {
            log.error("AI 调用失败", e);

            // 统一返回业务错误结构，避免将异常栈暴露给前端
            AIResponse response = new AIResponse();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());

            return Result.error(500, "AI 调用失败：" + e.getMessage(), response);
        }
    }

    /**
     * 按任务类型调用 AI
     * 根据任务类型选择对应的模型
     *
     * @param taskType 任务类型
     * @param request  AI 请求
     * @return AI 响应
     */
    @Override
    @PostMapping("/chat/{taskType}")
    public Result<AIResponse> chatByTaskType(
            @PathVariable String taskType,
            @Valid @RequestBody AIRequest request) {
        try {
            AICallCommand command = DTOConverter.toAppAICallCommand(request);
            command.setTaskType(taskType);

            AICallResult result = aiModelService.chatByTaskType(taskType, command);
            AIResponse response = DTOConverter.toApiAIResponse(result);

            return Result.success(response);
        } catch (Exception e) {
            log.error("AI 调用失败", e);

            // 统一错误响应格式，便于前端处理
            AIResponse response = new AIResponse();
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());

            return Result.error(500, "AI 调用失败：" + e.getMessage(), response);
        }
    }

    /**
     * 获取所有可用模型列表
     *
     * @return 模型列表
     */
    @Override
    @PostMapping("/models")
    public Result<List<ModelInfo>> getAvailableModels() {
        // 调用应用服务查询模型配置
        List<ModelConfig> models = modelConfigAppService.queryEnabledModels(new EnabledQuery(true));

        // 转换为 API DTO
        List<ModelInfo> modelInfos = models.stream()
                .map(model -> {
                    ModelInfo info = new ModelInfo();
                    info.setModelId(model.getId());
                    info.setModelName(model.getModelName());
                    info.setModelType(model.getModelType());
                    return info;
                })
                .collect(Collectors.toList());

        return Result.success(modelInfos);
    }

    /**
     * 获取推荐模型
     * 根据任务类型推荐最合适的模型
     *
     * @param taskType 任务类型
     * @return 推荐的模型信息
     */
    @Override
    @PostMapping("/models/recommend")
    public Result<ModelInfo> getRecommendedModel(@Valid @RequestBody ModelRecommendRequest request) {
        // 调用应用服务获取推荐模型
        ModelConfig model = modelConfigAppService.getRecommendedModel(new TaskTypeQuery(request.getTaskType()));

        if (model == null) {
            return Result.error(404, "未找到推荐模型");
        }

        // 转换为 API DTO
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setModelId(model.getId());
        modelInfo.setModelName(model.getModelName());
        modelInfo.setModelType(model.getModelType());

        return Result.success(modelInfo);
    }
}
