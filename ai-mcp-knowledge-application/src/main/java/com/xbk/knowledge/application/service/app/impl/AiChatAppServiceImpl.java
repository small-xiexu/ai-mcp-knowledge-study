package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.service.mcp.McpToolCatalogService;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.domain.model.adapter.repository.metrics.CallLogRepository;
import com.xbk.knowledge.domain.model.adapter.repository.chat.ChatSessionRepository;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.types.enums.CallStatus;
import com.xbk.knowledge.types.exception.BusinessException;
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
 * 支持同步与流式对话，并兼容 RAG
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatAppServiceImpl implements AiChatAppService {

    private static final String RAG_SYSTEM_PROMPT = "请根据【参考文档】部分的信息来回答用户的问题。\n"
            + "回答时要表现得像你本来就知道这些信息一样，不要提及\"根据文档\"之类的话。\n"
            + "如果文档中没有相关信息，请直接说\"我不太清楚这个问题\"。\n\n"
            + "【参考文档】\n"
            + "{documents}";

    private final ModelConfigAppService modelConfigAppService;
    private final ChatClientAssemblyService chatClientAssemblyService;
    private final RagVectorStoreService ragVectorStoreService;
    private final CallLogRepository callLogRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMemory chatMemory;
    private final McpToolCatalogService mcpToolCatalogService;

    /**
     * 同步对话
     *
     * 为什么：提供简单调用入口，便于同步场景接入
     * 入参：对话调用命令
     * 出参：调用结果
     */
    @Override
    public AICallResult chat(AICallCommand command) {
        long startTime = System.currentTimeMillis();
        /*
         * 目的：统一模型、工具、提示词准备流程
         */
        ModelConfig modelConfig = resolveChatModel(command);
        boolean toolEnabled = resolveToolEnabled(modelConfig);
        Prompt prompt = buildPrompt(command, toolEnabled);
        CallLog callLog = buildCallLog(modelConfig, command);
        String conversationId = resolveConversationId(command);
        GatewayToolBindingContextHolder.set(modelConfig.getId(), command.getSessionId());
        try {
            ChatClient chatClient = resolveChatClient(modelConfig, toolEnabled);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String content = extractContent(response);
            Usage usage = response != null && response.getMetadata() != null
                    ? response.getMetadata().getUsage()
                    : null;
            Integer tokensUsed = resolveTokensUsed(usage);
            long responseTime = System.currentTimeMillis() - startTime;

            /*
             * 目的：保存对话记忆，支持多轮上下文
             */
            appendChatMemory(conversationId, command.getContent(), content);
            CallLogAggregate aggregate = CallLogAggregate.builder()
                    .callLog(fillSuccessLog(callLog, content, tokensUsed, responseTime))
                    .build();
            callLogRepository.save(aggregate);

            return AICallResult.builder()
                    .success(true)
                    .content(content)
                    .modelUsed(modelConfig.getModelName())
                    .tokensUsed(tokensUsed)
                    .responseTime(responseTime)
                    .retryCount(0)
                    .fallback(false)
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            CallLogAggregate aggregate = CallLogAggregate.builder()
                    .callLog(fillFailureLog(callLog, e.getMessage(), responseTime))
                    .build();
            callLogRepository.save(aggregate);
            throw e;
        } finally {
            GatewayToolBindingContextHolder.clear();
        }
    }

    /**
     * 流式对话
     *
     * 为什么：满足前端流式渲染与大模型逐字输出场景
     * 入参：对话调用命令
     * 出参：流式响应
     */
    @Override
    public Flux<ChatResponse> streamChat(AICallCommand command) {
        long startTime = System.currentTimeMillis();
        /*
         * 目的：统一模型、工具、提示词准备流程
         */
        ModelConfig modelConfig = resolveChatModel(command);
        boolean toolEnabled = resolveToolEnabled(modelConfig);
        Prompt prompt = buildPrompt(command, toolEnabled);
        CallLog callLog = buildCallLog(modelConfig, command);
        UsageStats usageStats = new UsageStats();
        String conversationId = resolveConversationId(command);
        StringBuilder assistantBuffer = new StringBuilder();
        return Flux.defer(() -> {
            GatewayToolBindingContextHolder.set(modelConfig.getId(), command.getSessionId());
            ChatClient chatClient = resolveChatClient(modelConfig, toolEnabled);
            return chatClient.prompt(prompt)
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        captureUsage(response, usageStats);
                        appendStreamContent(response, assistantBuffer);
                    })
                    .doOnError(error -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        CallLogAggregate aggregate = CallLogAggregate.builder()
                                .callLog(fillFailureLog(callLog, error.getMessage(), responseTime))
                                .build();
                        callLogRepository.save(aggregate);
                    })
                    .doOnComplete(() -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        Integer tokensUsed = usageStats.getTotalTokens();
                        /*
                         * 目的：流式完成后统一落库与记忆追加
                         */
                        appendChatMemory(conversationId, command.getContent(), assistantBuffer.toString());
                        CallLogAggregate aggregate = CallLogAggregate.builder()
                                .callLog(fillSuccessLog(callLog, null, tokensUsed, responseTime))
                                .build();
                        callLogRepository.save(aggregate);
                    })
                    .doFinally(signalType -> GatewayToolBindingContextHolder.clear());
        });
    }

    /**
     * 解析对话模型
     *
     * 为什么：允许显式模型优先，未指定时使用全局激活模型
     * 入参：对话命令
     * 出参：模型配置
     */
    private ModelConfig resolveChatModel(AICallCommand command) {
        enforceSessionModelBinding(command);
        Long modelId = command.getModelId();
        if (modelId != null) {
            return modelConfigAppService.queryModelConfigById(new IdQuery(modelId));
        }
        ModelConfig activeChat = modelConfigAppService.getActiveChatModel();
        if (activeChat == null) {
            throw new IllegalStateException("未配置激活的对话模型");
        }
        return activeChat;
    }

    private void enforceSessionModelBinding(AICallCommand command) {
        if (command == null) {
            return;
        }
        Long sessionId = command.getSessionId();
        if (sessionId == null) {
            return;
        }
        ChatSession session = chatSessionRepository.findById(sessionId);
        if (session == null || session.getModelId() == null) {
            return;
        }
        Long sessionModelId = session.getModelId();
        Long requestModelId = command.getModelId();
        if (requestModelId != null && !sessionModelId.equals(requestModelId)) {
            String message = buildModelLockMessage(sessionModelId);
            throw new BusinessException(message);
        }
        if (requestModelId == null) {
            /*
             * 目的：会话已绑定模型时，强制使用会话模型
             */
            command.setModelId(sessionModelId);
        }
    }

    private String buildModelLockMessage(Long modelId) {
        String modelName = resolveModelName(modelId);
        String displayName = modelName != null ? modelName : String.valueOf(modelId);
        return "该会话已绑定模型【" + displayName + "】，为保证对话一致性不可切换模型。如需切换，请新建会话。";
    }

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
     *
     * 为什么：统一走装配服务，避免业务侧重复拼装模型和工具
     * 入参：模型配置、工具开关
     * 出参：ChatClient 实例
     */
    private ChatClient resolveChatClient(ModelConfig modelConfig, boolean toolEnabled) {
        return chatClientAssemblyService.buildChatClient(modelConfig, toolEnabled);
    }

    /**
     * 构建提示词
     *
     * 为什么：统一拼装历史消息、工具提示与 RAG 上下文
     * 入参：对话命令、工具开关
     * 出参：Prompt 对象
     */
    private Prompt buildPrompt(AICallCommand command, boolean toolEnabled) {
        String content = command.getContent();
        if (!StringUtils.hasText(content)) {
            content = "";
        }
        String conversationId = resolveConversationId(command);
        List<Message> messages = new ArrayList<>();
        if (conversationId != null) {
            List<Message> history = chatMemory.get(conversationId);
            if (!CollectionUtils.isEmpty(history)) {
                messages.addAll(history);
            }
        }
        if (toolEnabled) {
            String toolPrompt = resolveToolPrompt();
            if (StringUtils.hasText(toolPrompt)) {
                messages.add(new SystemMessage(toolPrompt));
            }
        }
        List<String> ragTags = command.getRagTags();
        if (CollectionUtils.isEmpty(ragTags)) {
            if (messages.isEmpty()) {
                return new Prompt(content);
            }
            messages.add(new UserMessage(content));
            return new Prompt(messages);
        }
        List<Document> documents = ragVectorStoreService.similaritySearch(content, ragTags);
        if (CollectionUtils.isEmpty(documents)) {
            if (messages.isEmpty()) {
                return new Prompt(content);
            }
            messages.add(new UserMessage(content));
            return new Prompt(messages);
        }
        /*
         * 目的：将检索到的文档注入系统提示，增强回答依据
         */
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
     *
     * 为什么：提示模型可调用的工具能力
     * 入参：无
     * 出参：工具提示词
     */
    private String resolveToolPrompt() {
        if (mcpToolCatalogService == null) {
            return "";
        }
        String prompt = mcpToolCatalogService.buildToolPrompt();
        return prompt != null ? prompt : "";
    }

    /**
     * 判断工具是否启用
     *
     * 为什么：配置未显式设置时默认开启工具能力
     * 入参：模型配置
     * 出参：是否启用
     */
    private boolean resolveToolEnabled(ModelConfig modelConfig) {
        if (modelConfig == null || modelConfig.getToolEnabled() == null) {
            return true;
        }
        return Boolean.TRUE.equals(modelConfig.getToolEnabled());
    }

    /**
     * 提取模型回复内容
     *
     * 为什么：统一处理空响应与空输出
     * 入参：ChatResponse
     * 出参：文本内容
     */
    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return "";
        }
        AssistantMessage output = response.getResult().getOutput();
        if (output == null) {
            return "";
        }
        String content = output.getText();
        return content != null ? content : "";
    }

    /**
     * 构建调用日志
     *
     * 为什么：为后续落库提供基础字段
     * 入参：模型配置、调用命令
     * 出参：日志实体
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
     *
     * 为什么：记录错误信息与耗时便于排查
     * 入参：日志实体、错误信息、耗时
     * 出参：日志实体
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
     * 解析 token 使用量
     *
     * 为什么：兼容不同模型返回的 token 字段
     * 入参：Usage
     * 出参：token 数
     */
    private Integer resolveTokensUsed(Usage usage) {
        if (usage == null) {
            return 0;
        }
        Integer totalTokens = usage.getTotalTokens();
        if (totalTokens != null) {
            return totalTokens;
        }
        Integer promptTokens = usage.getPromptTokens();
        Integer completionTokens = usage.getCompletionTokens();
        int prompt = promptTokens != null ? promptTokens : 0;
        int completion = completionTokens != null ? completionTokens : 0;
        return prompt + completion;
    }

    /**
     * 截断内容
     *
     * 为什么：控制日志体积，避免存储膨胀
     * 入参：内容、最大长度
     * 出参：截断内容
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
     *
     * 为什么：流式结果需要累积统计
     * 入参：响应、统计容器
     * 出参：无
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
     *
     * 为什么：流式场景需要累计文本用于落库与记忆
     * 入参：响应、缓存容器
     * 出参：无
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
     *
     * 为什么：支持多轮对话上下文
     * 入参：会话 ID、用户文本、助手文本
     * 出参：无
     */
    private void appendChatMemory(String conversationId, String userContent, String assistantContent) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        List<Message> messages = new ArrayList<>();
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
     *
     * 为什么：对话记忆以会话维度隔离
     * 入参：对话命令
     * 出参：会话 ID
     */
    private String resolveConversationId(AICallCommand command) {
        if (command == null || command.getSessionId() == null) {
            return null;
        }
        return String.valueOf(command.getSessionId());
    }

    /**
     * 流式 token 使用统计
     *
     * 为什么：流式响应在结束前无法一次性拿到完整 usage
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

        
        private Integer getTotalTokens() {
            if (totalTokens != null) {
                return totalTokens;
            }
            int prompt = promptTokens != null ? promptTokens : 0;
            int completion = completionTokens != null ? completionTokens : 0;
            return prompt + completion;
        }
    }
}
