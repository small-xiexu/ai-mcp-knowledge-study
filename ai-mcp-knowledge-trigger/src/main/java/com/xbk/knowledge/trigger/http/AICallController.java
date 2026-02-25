package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.IAICallService;
import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.ModelInfo;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.trigger.converter.DTOConverter;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * AI 调用 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 * <p>
 * 职责：HTTP 接口适配，用于转发应用层能力
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AICallController implements IAICallService {

    /**
     * 思考分片事件名。
     */
    private static final String THINKING_EVENT = "thinking";

    /**
     * Anthropic 思考分片签名字段。
     */
    private static final String THINKING_SIGNATURE_KEY = "signature";

    /**
     * 模型配置应用服务，用于查询可用模型列表。
     */
    private final ModelConfigAppService modelConfigAppService;

    /**
     * AI 对话应用服务，负责流式对话调用编排。
     */
    private final AiChatAppService aiChatAppService;

    /**
     * 流式 AI 对话
     * <p>
     * 使用 SSE 保证前端逐步渲染，提升长文本体验。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验并初始化 SSE 响应头。
     * 2. 将 API 请求转换为 `AICallCommand`，并初始化 usage 统计容器。
     * 3. 调用 `aiChatAppService.streamChat` 获取流式 `ChatResponse`。
     * 4. 对每个分片执行内容发送与 token 统计，结束时推送 usage 事件并关闭连接。
     * 5. 异常场景通过 `SseEmitter.completeWithError` 终止流并交由上层处理。
     * 
     * @param request AI 请求
     * @param httpResponse HTTP 响应。
     * @return SSE 响应
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission("agent:read")
    @Override
    public SseEmitter stream(@Valid @RequestBody AIRequest request, HttpServletResponse httpResponse) {
        // SSE 响应基础设置
        // 1、UTF-8 防止中文分片乱码
        // 2、no-cache/keep-alive 让连接保持长连
        // 3、X-Accel-Buffering=no 禁止反向代理缓冲，确保分片实时推送
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Connection", "keep-alive");
        httpResponse.setHeader("X-Accel-Buffering", "no");

        // 0L 表示不设置超时，由业务自行在 onComplete/onError 显式结束连接
        SseEmitter emitter = new SseEmitter(0L);
        AICallCommand command = DTOConverter.toAppAICallCommand(request);

        // 记录整次流式话的 token 统计，最终在结束阶段一次性回传 usage 事件
        UsageStats usageStats = new UsageStats();

        // subscribe 三段回调语义
        // onNext(每个分片)、onError(异常终止)、onComplete(正常结束)
        Consumer<ChatResponse> onChunk = chatResponse -> {
            // 每收到一个分片先提取 usage 快照（若当前分片不含 usage，方法内部安全忽略）
            captureUsage(chatResponse, usageStats);
            // 将当前分片文本通过 SSE 发送给前端，前端可实时拼接渲染
            sendChunk(emitter, chatResponse);
        };
        // 一旦流式调用出现异常，立即以错误态结束 SSE 连接并把异常抛给前端
        Consumer<Throwable> onError = emitter::completeWithError;
        Runnable onComplete = () -> {
            // 流结束时回传累计 usage，便于前端展示 token 消耗
            sendUsage(emitter, usageStats);
            // 明确关闭 SSE 连接，通知前端“已完成”
            emitter.complete();
        };
        // 调用应用服务执行流式对话，并传入三段回调逻辑
        aiChatAppService
                .streamChat(command)
                .subscribe(onChunk, onError, onComplete);
        return emitter;
    }

    /**
     * 获取所有可用模型列表
     * <p>
     * 前端下拉统一来源，避免直接访问配置表。
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Controller 以 `enabled=true` 条件调用 `modelConfigAppService.queryEnabledModels`。
     * 3. 将领域模型配置映射为对外 `ModelInfo` 列表。
     * 4. 统一封装 `Result.success` 返回，供前端模型选择器使用。
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

    /**
     * 发送单个流式分片。
     *
     * @param emitter SSE 发送器
     * @param response 当前分片响应
     */
    private void sendChunk(SseEmitter emitter, ChatResponse response) {
        try {
            // 1、分片或结果对象为空时直接忽略，避免空指针
            if (response == null || response.getResult() == null) {
                return;
            }

            // 2、仅处理 Assistant 输出文本，非文本分片直接跳过
            AssistantMessage output = response.getResult().getOutput();
            if (output == null) {
                return;
            }
            String text = output.getText();
            if (!StringUtils.hasText(text)) {
                return;
            }

            // 3、思考分片发送为 thinking 事件，最终回答保持默认 message 事件
            if (isThinkingChunk(output)) {
                emitter.send(SseEmitter.event().name(THINKING_EVENT).data(text));
                return;
            }

            // 4、按默认 message 事件推送文本分片，前端可实时拼接
            emitter.send(SseEmitter.event().data(text));
        } catch (IOException e) {
            // 5、连接写出失败时终止 SSE，避免僵尸连接
            emitter.completeWithError(e);
        }
    }

    /**
     * 判断当前分片是否属于模型思考内容。
     *
     * @param output 助手分片消息。
     * @return `true` 表示思考分片，`false` 表示最终回答分片。
     */
    private boolean isThinkingChunk(AssistantMessage output) {
        if (output == null || output.getMetadata() == null) {
            return false;
        }
        return output.getMetadata().containsKey(THINKING_SIGNATURE_KEY);
    }

    /**
     * 捕获并累计 usage 快照。
     *
     * @param response 当前分片响应
     * @param usageStats usage 累计容器
     */
    private void captureUsage(ChatResponse response, UsageStats usageStats) {
        // 1、metadata 缺失时直接返回（部分分片可能不携带 usage）
        if (response == null || response.getMetadata() == null) {
            return;
        }

        // 2、提取当前分片 usage；为空则忽略
        Usage usage = response.getMetadata().getUsage();
        if (usage == null) {
            return;
        }

        // 3、写入累计容器（仅覆盖非 null 字段）
        usageStats.update(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    /**
     * 发送整次流式调用的 usage 事件。
     *
     * @param emitter SSE 发送器
     * @param usageStats usage 累计容器
     */
    private void sendUsage(SseEmitter emitter, UsageStats usageStats) {
        // 1、没有可用统计值时不发送 usage 事件
        if (!usageStats.hasData()) {
            return;
        }
        try {
            // 2、组装前端约定的 usage 载荷
            Map<String, Integer> payload = new HashMap<>();
            payload.put("promptTokens", usageStats.getPromptTokens());
            payload.put("completionTokens", usageStats.getCompletionTokens());
            payload.put("totalTokens", usageStats.getTotalTokens());

            // 3、以命名事件 usage 推送，便于前端按事件类型处理
            emitter.send(SseEmitter.event().name("usage").data(payload));
        } catch (IOException e) {
            // 4、发送失败时终止 SSE
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
        /**
         * 输入 token 数（Prompt）。
         */
        private Integer promptTokens;

        /**
         * 输出 token 数（Completion）。
         */
        private Integer completionTokens;

        /**
         * 总 token 数（Prompt + Completion）。
         */
        private Integer totalTokens;

        /**
         * 更新业务数据。
         * 
         * @param promptTokens 输入Token数。
         * @param completionTokens 输出Token数。
         * @param totalTokens 总Token数。
         */
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
