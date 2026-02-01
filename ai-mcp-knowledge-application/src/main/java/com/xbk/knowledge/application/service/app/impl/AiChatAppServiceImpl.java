package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.service.mcp.McpToolCatalogService;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.types.enums.CallStatus;
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
import org.springframework.ai.tool.ToolCallbackProvider;
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
    private final ModelProviderFactory modelProviderFactory;
    private final RagVectorStoreService ragVectorStoreService;
    private final CallLogRepository callLogRepository;
    private final ChatMemory chatMemory;
    private final McpToolCatalogService mcpToolCatalogService;
    private final ToolCallbackProvider toolCallbackProvider;

    @Override
    public AICallResult chat(AICallCommand command) {
        long startTime = System.currentTimeMillis();
        ModelConfig modelConfig = resolveChatModel(command);
        boolean toolEnabled = resolveToolEnabled(modelConfig);
        Prompt prompt = buildPrompt(command, toolEnabled);
        CallLog callLog = buildCallLog(modelConfig, command);
        String conversationId = resolveConversationId(command);
        try {
            ChatClient chatClient = resolveChatClient(modelConfig, toolEnabled);
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String content = extractContent(response);
            Usage usage = response != null && response.getMetadata() != null
                    ? response.getMetadata().getUsage()
                    : null;
            Integer tokensUsed = resolveTokensUsed(usage);
            long responseTime = System.currentTimeMillis() - startTime;

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
        }
    }

    @Override
    public Flux<ChatResponse> streamChat(AICallCommand command) {
        long startTime = System.currentTimeMillis();
        ModelConfig modelConfig = resolveChatModel(command);
        boolean toolEnabled = resolveToolEnabled(modelConfig);
        Prompt prompt = buildPrompt(command, toolEnabled);
        CallLog callLog = buildCallLog(modelConfig, command);
        UsageStats usageStats = new UsageStats();
        String conversationId = resolveConversationId(command);
        StringBuilder assistantBuffer = new StringBuilder();
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
                    appendChatMemory(conversationId, command.getContent(), assistantBuffer.toString());
                    CallLogAggregate aggregate = CallLogAggregate.builder()
                            .callLog(fillSuccessLog(callLog, null, tokensUsed, responseTime))
                            .build();
                    callLogRepository.save(aggregate);
                });
    }

    private ModelConfig resolveChatModel(AICallCommand command) {
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

    private ChatClient resolveChatClient(ModelConfig modelConfig, boolean toolEnabled) {
        ChatClient chatClient = modelProviderFactory.createChatClient(modelConfig);
        if (toolEnabled && toolCallbackProvider != null) {
            chatClient = chatClient.mutate()
                    .defaultToolCallbacks(toolCallbackProvider)
                    .build();
        }
        return chatClient;
    }

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
        String documentText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        Message ragMessage = new SystemPromptTemplate(RAG_SYSTEM_PROMPT)
                .createMessage(Map.of("documents", documentText));
        messages.add(ragMessage);
        messages.add(new UserMessage(content));
        return new Prompt(messages);
    }

    private String resolveToolPrompt() {
        if (mcpToolCatalogService == null) {
            return "";
        }
        String prompt = mcpToolCatalogService.buildToolPrompt();
        return prompt != null ? prompt : "";
    }

    private boolean resolveToolEnabled(ModelConfig modelConfig) {
        if (modelConfig == null || modelConfig.getToolEnabled() == null) {
            return true;
        }
        return Boolean.TRUE.equals(modelConfig.getToolEnabled());
    }

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

    private CallLog buildCallLog(ModelConfig modelConfig, AICallCommand command) {
        Long modelId = modelConfig.getId();
        String taskType = command.getTaskType();
        String requestContent = truncateContent(command.getContent(), 5000);
        return CallLog.builder()
                .modelId(modelId)
                .taskType(taskType)
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

    private CallLog fillFailureLog(CallLog callLog, String errorMessage, long responseTime) {
        callLog.setResponseContent(null);
        callLog.setTokensUsed(0);
        callLog.setResponseTime(responseTime);
        callLog.setStatus(CallStatus.FAILED);
        callLog.setErrorMessage(errorMessage);
        callLog.setCreatedAt(LocalDateTime.now());
        return callLog;
    }

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

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
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

    private String resolveConversationId(AICallCommand command) {
        if (command == null || command.getSessionId() == null) {
            return null;
        }
        return String.valueOf(command.getSessionId());
    }

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
