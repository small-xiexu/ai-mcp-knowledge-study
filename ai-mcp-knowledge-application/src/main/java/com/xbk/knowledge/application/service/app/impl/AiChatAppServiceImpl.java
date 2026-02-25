package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.service.mcp.McpToolCatalogService;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.metrics.model.aggregate.CallLogAggregate;
import com.xbk.knowledge.domain.metrics.model.entity.CallLog;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.chat.model.entity.ChatSession;
import com.xbk.knowledge.domain.metrics.adapter.repository.CallLogRepository;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatSessionRepository;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.types.enums.CallStatus;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * AI 对话应用服务实现
 * 支持流式对话，并兼容 RAG
 * <p>
 * 职责：应用层用例实现，用于协调领域能力
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatAppServiceImpl implements AiChatAppService {

    /**
     * RAG 系统提示词模板。
     */
    private static final String RAG_SYSTEM_PROMPT = "请根据【参考文档】部分的信息来回答用户的问题。\n"
            + "回答时要表现得像你本来就知道这些信息一样，不要提及\"根据文档\"之类的话。\n"
            + "如果文档中没有相关信息，请直接说\"我不太清楚这个问题\"。\n\n"
            + "【参考文档】\n"
            + "{documents}";

    /**
     * 模型配置应用服务。
     */
    private final ModelConfigAppService modelConfigAppService;

    /**
     * ChatClient 组装服务。
     */
    private final ChatClientAssemblyService chatClientAssemblyService;

    /**
     * RAG 向量检索服务。
     */
    private final RagVectorStoreService ragVectorStoreService;

    /**
     * 调用日志仓储。
     */
    private final CallLogRepository callLogRepository;

    /**
     * 聊天会话仓储。
     */
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 聊天记忆组件。
     */
    private final ChatMemory chatMemory;

    /**
     * MCP 工具目录服务。
     */
    private final McpToolCatalogService mcpToolCatalogService;

    /**
     * 流式对话
     * <p>
     * 满足前端流式渲染与大模型逐字输出场景
     * 
     * @param command AI 调用命令。
     * @return 流式响应序列。
     */
    @Override
    public Flux<ChatResponse> streamChat(AICallCommand command) {
        long startTime = System.currentTimeMillis();
        // 1、按统一顺序准备调用上下文，避免流式行为漂移
        ChatCallContext context = prepareChatContext(command);
        ModelConfig modelConfig = context.modelConfig;
        boolean toolEnabled = context.toolEnabled;
        Prompt prompt = context.prompt;
        CallLog callLog = context.callLog;
        UsageStats usageStats = new UsageStats();
        String conversationId = context.conversationId;
        StringBuilder assistantBuffer = new StringBuilder();
        return Flux.defer(() -> {
            // 2、每次订阅都创建独立 runId，确保并发流请求隔离
            String runId = TraceIdUtils.getOrCreateTraceId();
            GatewayToolBindingContextHolder.set(modelConfig.getId(), command.getSessionId(), null, runId, null);
            ChatClient chatClient = resolveChatClient(modelConfig, toolEnabled);
            return chatClient.prompt(prompt)
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        // 分片回调阶段持续累积 usage 与输出文本
                        captureUsage(response, usageStats);
                        appendStreamContent(response, assistantBuffer);
                    })
                    .doOnError(error -> {
                        // 异常立即落失败日志，保证流式中断可排查
                        long responseTime = System.currentTimeMillis() - startTime;
                        CallLogAggregate aggregate = CallLogAggregate.builder()
                                .callLog(fillFailureLog(callLog, error.getMessage(), responseTime))
                                .build();
                        callLogRepository.save(aggregate);
                    })
                    .doOnComplete(() -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        Integer tokensUsed = usageStats.getTotalTokens();
                        // 完成时再统一落库与写记忆，确保内容完整
                        appendChatMemory(conversationId, command.getContent(), assistantBuffer.toString());
                        CallLogAggregate aggregate = CallLogAggregate.builder()
                                .callLog(fillSuccessLog(callLog, null, tokensUsed, responseTime))
                                .build();
                        callLogRepository.save(aggregate);
                    })
                    .doFinally(signalType -> GatewayToolBindingContextHolder.clear());
        });
    }

    private ChatCallContext prepareChatContext(AICallCommand command) {
        // 1、确定本次调用使用的模型
        ModelConfig modelConfig = resolveChatModel(command);
        // 2、根据模型配置计算工具开关
        boolean toolEnabled = resolveToolEnabled(modelConfig);
        // 3、基于用户输入 + 历史记忆 + RAG 组装 Prompt
        Prompt prompt = buildPrompt(command, toolEnabled);
        // 4、创建调用日志骨架，后续只补充结果字段
        CallLog callLog = buildCallLog(modelConfig, command);
        // 5、计算会话 ID，供记忆读写使用
        String conversationId = resolveConversationId(command);
        return new ChatCallContext(modelConfig, toolEnabled, prompt, callLog, conversationId);
    }

    /**
     * 解析对话模型
     * <p>
     * 允许显式模型优先，未指定时使用全局激活模型
     * 
     * @param command AI 调用命令。
     * @return 当前对话使用的模型配置。
     */
    private ModelConfig resolveChatModel(AICallCommand command) {
        // 优先执行话模型锁定规则必要时改写 command.modelId 或直接拒绝
        enforceSessionModelBinding(command);
        Long modelId = command.getModelId();
        // 调用方显式指定模型时优先使用显式值
        if (modelId != null) {
            return modelConfigAppService.queryModelConfigById(new IdQuery(modelId));
        }
        // 未指定时回退到系统当前激活的对话模型
        ModelConfig activeChat = modelConfigAppService.getActiveChatModel();
        if (activeChat == null) {
            throw new IllegalStateException("未配置激活的对话模型");
        }
        return activeChat;
    }

    /**
     * 校验并绑定话模型一致性。
     * 
     * @param command 对话调用指令。
     */
    private void enforceSessionModelBinding(AICallCommand command) {
        if (command == null) {
            return;
        }
        Long sessionId = command.getSessionId();
        // 未开启会话上下文时不触发模型锁定
        if (sessionId == null) {
            return;
        }
        ChatSession session = chatSessionRepository.findById(sessionId);
        // 话未绑定模型时允许按普通策略选模
        if (session == null || session.getModelId() == null) {
            return;
        }
        Long sessionModelId = session.getModelId();
        Long requestModelId = command.getModelId();
        // 请求模型与话已绑定模型冲突时，明确拒绝以保证话一致性
        if (requestModelId != null && !sessionModelId.equals(requestModelId)) {
            String message = buildModelLockMessage(sessionModelId);
            throw new BusinessException(message);
        }
        if (requestModelId == null) {
            // 话已绑定模型时，强制使用话模型
            command.setModelId(sessionModelId);
        }
    }

    private String buildModelLockMessage(Long modelId) {
        String modelName = resolveModelName(modelId);
        String displayName = modelName != null ? modelName : String.valueOf(modelId);
        return "该话已绑定模型【" + displayName + "】，为保证对话一致性不可切换模型。如需切换，请新建话。";
    }

    /**
     * 解析模型名称。
     * 
     * @param modelId 模型ID。
     * @return 名称文本。
     */
    private String resolveModelName(Long modelId) {
        if (modelId == null) {
            return null;
        }
        ModelConfig modelConfig = modelConfigAppService.queryModelConfigById(new IdQuery(modelId));
        if (modelConfig == null) {
            return null;
        }
        return modelConfig.getModelName();
    }

    /**
     * 解析 ChatClient
     * <p>
     * 统一走装配服务，避免业务侧重复拼装模型和工具
     * 
     * @param modelConfig 模型配置。
     * @param toolEnabled 是否启用工具。
     * @return 可执行对话请求的 ChatClient。
     */
    private ChatClient resolveChatClient(ModelConfig modelConfig, boolean toolEnabled) {
        return chatClientAssemblyService.buildChatClient(modelConfig, toolEnabled);
    }

    /**
     * 构建提示词
     * <p>
     * 统一拼装历史消息、工具提示与 RAG 上下文
     * 
     * @param command AI 调用命令。
     * @param toolEnabled 是否启用工具。
     * @return 组装后的提示词。
     */
    private Prompt buildPrompt(AICallCommand command, boolean toolEnabled) {
        String content = command.getContent();
        if (!StringUtils.hasText(content)) {
            content = "";
        }
        String conversationId = resolveConversationId(command);
        List<Message> messages = new ArrayList<>();
        // 1、先注入话历史，保证多轮对话连续性
        if (conversationId != null) {
            List<Message> history = chatMemory.get(conversationId);
            if (!CollectionUtils.isEmpty(history)) {
                messages.addAll(history);
            }
        }
        // 2、工具开启时注入工具提示词，引导模型按规范调用工具
        if (toolEnabled) {
            String toolPrompt = resolveToolPrompt();
            if (StringUtils.hasText(toolPrompt)) {
                messages.add(new SystemMessage(toolPrompt));
            }
        }
        List<String> ragTags = command.getRagTags();
        // 3、未启用 RAG 时直接走纯对话路径
        if (CollectionUtils.isEmpty(ragTags)) {
            if (messages.isEmpty()) {
                return new Prompt(content);
            }
            messages.add(new UserMessage(content));
            return new Prompt(messages);
        }
        // 4、启用 RAG 时先检索语料，检索为空则回退到纯对话
        List<Document> documents = ragVectorStoreService.similaritySearch(content, ragTags);
        if (CollectionUtils.isEmpty(documents)) {
            if (messages.isEmpty()) {
                return new Prompt(content);
            }
            messages.add(new UserMessage(content));
            return new Prompt(messages);
        }
        // 将检索到的文档注入系统提示，增强回答依据
        String documentText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        Message ragMessage = new SystemPromptTemplate(RAG_SYSTEM_PROMPT)
                .createMessage(Map.of("documents", documentText));
        messages.add(ragMessage);
        messages.add(new UserMessage(content));
        return new Prompt(messages);
    }

    /**
     * 构建工具提示词
     * <p>
     * 提示模型可调用的工具能力
     * 
     * @return 工具提示词文本。
     */
    private String resolveToolPrompt() {
        if (mcpToolCatalogService == null) {
            return "";
        }
        // 防御空返回，避免后续系统消息出现 null 文本
        String prompt = mcpToolCatalogService.buildToolPrompt();
        return prompt != null ? prompt : "";
    }

    /**
     * 判断工具是否启用
     * <p>
     * 配置未显式设置时默认开启工具能力
     * 
     * @param modelConfig 模型配置。
     * @return `true` 表示启用工具，`false` 表示禁用工具。
     */
    private boolean resolveToolEnabled(ModelConfig modelConfig) {
        if (modelConfig == null || modelConfig.getToolEnabled() == null) {
            return true;
        }
        return modelConfig.getToolEnabled();
    }

    /**
     * 构建调用日志
     * <p>
     * 为后续落库提供基础字段
     * 
     * @param modelConfig 模型配置。
     * @param command AI 调用命令。
     * @return 调用日志骨架。
     */
    private CallLog buildCallLog(ModelConfig modelConfig, AICallCommand command) {
        Long modelId = modelConfig.getId();
        String requestContent = truncateContent(command.getContent(), 5000);
        return CallLog.builder()
                .modelId(modelId)
                .requestContent(requestContent)
                .build();
    }

    private CallLog fillSuccessLog(CallLog callLog, String responseContent, Integer tokensUsed, long responseTime) {
        callLog.setResponseContent(truncateContent(responseContent, 5000));
        callLog.setTokensUsed(tokensUsed != null ? tokensUsed : 0);
        callLog.setResponseTime(responseTime);
        callLog.setStatus(CallStatus.SUCCESS);
        callLog.setCreatedAt(LocalDateTime.now());
        return callLog;
    }

    /**
     * 填充失败日志
     * <p>
     * 记录错误信息与耗时便于排查
     * 
     * @param callLog 调用日志骨架。
     * @param errorMessage 错误信息。
     * @param responseTime 响应耗时（毫秒）。
     * @return 填充后的失败调用日志。
     */
    private CallLog fillFailureLog(CallLog callLog, String errorMessage, long responseTime) {
        callLog.setResponseContent(null);
        callLog.setTokensUsed(0);
        callLog.setResponseTime(responseTime);
        callLog.setStatus(CallStatus.FAILED);
        callLog.setErrorMessage(errorMessage);
        callLog.setCreatedAt(LocalDateTime.now());
        return callLog;
    }

    /**
     * 截断内容
     * <p>
     * 控制日志体积，避免存储膨胀
     * 
     * @param content 消息内容。
     * @param maxLength 最大长度。
     * @return 截断后的文本内容。
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
     * 记录流式返回的 token 使用
     * <p>
     * 流式结果需要累积统计
     * 
     * @param response 模型响应分片。
     * @param usageStats token 统计容器。
     */
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

    /**
     * 追加流式输出内容
     * <p>
     * 流式场景需要累计文本用于落库与记忆
     * 
     * @param response 模型响应分片。
     * @param buffer 助手输出累积缓冲区。
     */
    private void appendStreamContent(ChatResponse response, StringBuilder buffer) {
        if (response == null || response.getResult() == null) {
            return;
        }
        AssistantMessage output = response.getResult().getOutput();
        if (output == null || output.getText() == null) {
            return;
        }
        buffer.append(output.getText());
    }

    /**
     * 追加对话记忆
     * <p>
     * 支持多轮对话上下文
     * 
     * @param conversationId 会话标识。
     * @param userContent 用户输入内容。
     * @param assistantContent 助手输出内容。
     */
    private void appendChatMemory(String conversationId, String userContent, String assistantContent) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        List<Message> messages = new ArrayList<>();
        // 仅追加本轮有效消息，避免写入空白消息污染历史
        if (StringUtils.hasText(userContent)) {
            messages.add(new UserMessage(userContent));
        }
        if (assistantContent != null) {
            messages.add(new AssistantMessage(assistantContent));
        }
        if (messages.isEmpty()) {
            return;
        }
        chatMemory.add(conversationId, messages);
    }

    /**
     * 解析会话 ID
     * <p>
     * 对话记忆以话维度隔离
     * 
     * @param command AI 调用命令。
     * @return 会话标识字符串。
     */
    private String resolveConversationId(AICallCommand command) {
        if (command == null || command.getSessionId() == null) {
            return null;
        }
        return String.valueOf(command.getSessionId());
    }

    private static final class ChatCallContext {

        /**
         * 本次调用使用的模型配置。
         */
        private final ModelConfig modelConfig;

        /**
         * 工具开关状态。
         */
        private final boolean toolEnabled;

        /**
         * 组装后的提示词对象。
         */
        private final Prompt prompt;

        /**
         * 调用日志骨架。
         */
        private final CallLog callLog;

        /**
         * 会话上下文 ID。
         */
        private final String conversationId;

        /**
         * 构建对话调用上下文。
         * 
         * @param modelConfig 模型配置。
         * @param toolEnabled 工具开关。
         * @param prompt 提示词。
         * @param callLog 调用日志。
         * @param conversationId 会话上下文 ID。
         */
        private ChatCallContext(ModelConfig modelConfig,
                                boolean toolEnabled,
                                Prompt prompt,
                                CallLog callLog,
                                String conversationId) {
            this.modelConfig = modelConfig;
            this.toolEnabled = toolEnabled;
            this.prompt = prompt;
            this.callLog = callLog;
            this.conversationId = conversationId;
        }
    }

    /**
     * 流式 token 使用统计
     * <p>
     * 流式响应在结束前无法一次性拿到完整 usage
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
            // 流式分片重复回调，保留最近一次非空 usage 视图
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

        /**
         * 计算本次响应的总 Token 数。
         * 
         * @return 总 Token 数。
         */
        private Integer getTotalTokens() {
            if (totalTokens != null) {
                return totalTokens;
            }
            // 某些模型只返回分项 token 时，用分项和作为最终统计
            int prompt = promptTokens != null ? promptTokens : 0;
            int completion = completionTokens != null ? completionTokens : 0;
            return prompt + completion;
        }
    }
}
