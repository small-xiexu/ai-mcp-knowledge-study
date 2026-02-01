package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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

    @Override
    public AICallResult chat(AICallCommand command) {
        ModelConfig modelConfig = resolveChatModel(command);
        ChatModel chatModel = resolveChatModelInstance(modelConfig);
        Prompt prompt = buildPrompt(command);
        ChatResponse response = chatModel.call(prompt);
        String content = extractContent(response);
        return AICallResult.builder()
                .success(true)
                .content(content)
                .modelUsed(modelConfig.getModelName())
                .tokensUsed(0)
                .retryCount(0)
                .fallback(false)
                .build();
    }

    @Override
    public Flux<ChatResponse> streamChat(AICallCommand command) {
        ModelConfig modelConfig = resolveChatModel(command);
        ChatModel chatModel = resolveChatModelInstance(modelConfig);
        Prompt prompt = buildPrompt(command);
        return chatModel.stream(prompt);
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

    private ChatModel resolveChatModelInstance(ModelConfig modelConfig) {
        ModelProvider provider = modelProviderFactory.getProvider(modelConfig.getModelType());
        return provider.createChatModel(modelConfig);
    }

    private Prompt buildPrompt(AICallCommand command) {
        String content = command.getContent();
        if (!StringUtils.hasText(content)) {
            content = "";
        }
        List<String> ragTags = command.getRagTags();
        if (CollectionUtils.isEmpty(ragTags)) {
            return new Prompt(content);
        }
        List<Document> documents = ragVectorStoreService.similaritySearch(content, ragTags);
        if (CollectionUtils.isEmpty(documents)) {
            return new Prompt(content);
        }
        String documentText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        Message ragMessage = new SystemPromptTemplate(RAG_SYSTEM_PROMPT)
                .createMessage(Map.of("documents", documentText));
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(content));
        messages.add(ragMessage);
        return new Prompt(messages);
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
}
