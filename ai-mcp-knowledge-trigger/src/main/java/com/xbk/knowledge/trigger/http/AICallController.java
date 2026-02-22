package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.api.IAICallService;
import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.ai.ModelInfo;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * AI 调用 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 * <p>
 * 职责：HTTP 接口适配，用于转发应用层能力
 *
 * @author sxie
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AICallController implements IAICallService {

    private final ModelConfigAppService modelConfigAppService;
    private final AiChatAppService aiChatAppService;

    /**
     * 通用 AI 调用接口
     * 根据策略自动选择最优模型
     *
     * 为什么：统一入口封装模型选择与调用流程，避免调用方直接拼装命令。
     *
     * @param request AI 请求
     * @return AI 响应
     */
    @Override
    @PostMapping("/chat")
    @SaCheckPermission("agent:read")
    public Result<AIResponse> chat(@Valid @RequestBody AIRequest request) {
        try {
            
            AICallCommand command = DTOConverter.toAppAICallCommand(request);
            AICallResult result = aiChatAppService.chat(command);
            AIResponse response = DTOConverter.toApiAIResponse(result);

            return Result.success(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用失败", e);

            
            AIResponse response = new AIResponse();
            response.setSuccess(false);
            String errorMessage = e.getMessage();
            response.setErrorMessage(errorMessage);

            String responseMessage = "AI 调用失败：" + errorMessage;
            return Result.error(ResultCode.AI_CALL_FAILED, responseMessage, response);
        }
    }

    /**
     * 流式 AI 对话
     *
     * 为什么：使用 SSE 保证前端逐步渲染，提升长文本体验。
     *
     * @param request AI 请求
     * @return SSE 响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission("agent:read")
    @Override
    public SseEmitter stream(@Valid @RequestBody AIRequest request, HttpServletResponse httpResponse) {
        
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Connection", "keep-alive");
        httpResponse.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(0L);
        AICallCommand command = DTOConverter.toAppAICallCommand(request);
        UsageStats usageStats = new UsageStats();
        
        aiChatAppService.streamChat(command).subscribe(
                chatResponse -> {
                    captureUsage(chatResponse, usageStats);
                    sendChunk(emitter, chatResponse);
                },
                emitter::completeWithError,
                () -> {
                    sendUsage(emitter, usageStats);
                    emitter.complete();
                }
        );
        return emitter;
    }

    /**
     * 获取所有可用模型列表
     *
     * 为什么：前端下拉统一来源，避免直接访问配置表。
     *
     * @return 模型列表
     */
    @Override
    @PostMapping("/models")
    @SaCheckPermission("agent:read")
    public Result<List<ModelInfo>> getAvailableModels() {
        // 调用应用服务查询模型配置
        EnabledQuery enabledQuery = new EnabledQuery(true);
        List<ModelConfig> models = modelConfigAppService.queryEnabledModels(enabledQuery);

        // 转换为 API DTO
        Collector<ModelInfo, ?, List<ModelInfo>> toListCollector = Collectors.toList();
        Function<ModelConfig, ModelInfo> modelInfoMapper = model -> {
            ModelInfo info = new ModelInfo();
            Long modelId = model.getId();
            String modelName = model.getModelName();
            ModelType modelType = model.getModelType();
            info.setModelId(modelId);
            info.setModelName(modelName);
            info.setModelType(modelType);
            return info;
        };
        List<ModelInfo> modelInfos = models
                .stream()
                .map(modelInfoMapper)
                .collect(toListCollector);

        return Result.success(modelInfos);
    }

    private void sendChunk(SseEmitter emitter, ChatResponse response) {
        try {
            if (response == null || response.getResult() == null) {
                return;
            }
            AssistantMessage output = response.getResult().getOutput();
            if (output == null || output.getText() == null) {
                return;
            }
            emitter.send(SseEmitter.event().data(output.getText()));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void captureUsage(ChatResponse response, UsageStats usageStats) {
        if (response == null || response.getMetadata() == null) {
            return;
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        usageStats.update(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private void sendUsage(SseEmitter emitter, UsageStats usageStats) {
        if (!usageStats.hasData()) {
            return;
        }
        try {
            Map<String, Integer> payload = new HashMap<>();
            payload.put("promptTokens", usageStats.getPromptTokens());
            payload.put("completionTokens", usageStats.getCompletionTokens());
            payload.put("totalTokens", usageStats.getTotalTokens());
            emitter.send(SseEmitter.event().name("usage").data(payload));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * 流式统计 usage 快照
     * 仅保存最新的 token 统计值，便于在流结束时回传
     *
     * @author sxie
     */
      private static class UsageStats {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        private void update(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
            if (promptTokens != null) {
                this.promptTokens = promptTokens;
            }
            if (completionTokens != null) {
                this.completionTokens = completionTokens;
            }
            if (totalTokens != null) {
                this.totalTokens = totalTokens;
            }
        }

        private boolean hasData() {
            return promptTokens != null || completionTokens != null || totalTokens != null;
        }

        private Integer getPromptTokens() {
            return promptTokens;
        }

        private Integer getCompletionTokens() {
            return completionTokens;
        }

        private Integer getTotalTokens() {
            return totalTokens;
        }
    }
}
