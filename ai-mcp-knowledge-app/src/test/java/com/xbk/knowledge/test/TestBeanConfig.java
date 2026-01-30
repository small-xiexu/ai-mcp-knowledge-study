package com.xbk.knowledge.test;

import java.util.List;
import java.util.Collections;

import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.trigger.job.MCPServerCSDNJob;
import com.xbk.knowledge.types.enums.ModelType;

/**
 * 测试环境的 Bean 替身
 * 避免外部依赖导致 Spring 上下文加载失败
 *
 * @author xiexu
 */
@Configuration
@Profile("test")
public class TestBeanConfig {

    /**
     * Mock OpenAI ChatModel
     *
     * @return OpenAiChatModel
     */
    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel() {
        OpenAiChatModel mock = Mockito.mock(OpenAiChatModel.class);
        AssistantMessage assistantMessage = new AssistantMessage("mock-response");
        Generation generation = new Generation(assistantMessage);
        List<Generation> generations = Collections.singletonList(generation);
        ChatResponse response = new ChatResponse(generations);
        Flux<ChatResponse> responseFlux = Flux.just(response);
        Mockito.when(mock.call(Mockito.any(Prompt.class))).thenReturn(response);
        Mockito.when(mock.stream(Mockito.any(Prompt.class))).thenReturn(responseFlux);
        return mock;
    }

    /**
     * Mock OpenAI API
     *
     * @return OpenAiApi
     */
    @Bean
    @Primary
    public OpenAiApi openAiApi() {
        return Mockito.mock(OpenAiApi.class);
    }

    /**
     * Mock 简单向量存储
     *
     * @return SimpleVectorStore
     */
    @Bean(name = "openAiSimpleVectorStore")
    @Primary
    public SimpleVectorStore openAiSimpleVectorStore() {
        SimpleVectorStore mock = Mockito.mock(SimpleVectorStore.class);
        List<Document> emptyDocuments = Collections.emptyList();
        Mockito.when(mock.similaritySearch(Mockito.any(SearchRequest.class))).thenReturn(emptyDocuments);
        return mock;
    }

    /**
     * Mock PgVector 向量存储
     *
     * @return PgVectorStore
     */
    @Bean(name = "openAiPgVectorStore")
    @Primary
    public PgVectorStore openAiPgVectorStore() {
        PgVectorStore mock = Mockito.mock(PgVectorStore.class);
        List<Document> emptyDocuments = Collections.emptyList();
        Mockito.when(mock.similaritySearch(Mockito.any(SearchRequest.class))).thenReturn(emptyDocuments);
        return mock;
    }

    /**
     * TokenTextSplitter
     *
     * @return TokenTextSplitter
     */
    @Bean
    @Primary
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    /**
     * Mock Ollama ChatModel
     *
     * @return OllamaChatModel
     */
    @Bean
    @Primary
    public OllamaChatModel ollamaChatModel() {
        OllamaChatModel mock = Mockito.mock(OllamaChatModel.class);
        AssistantMessage assistantMessage = new AssistantMessage("mock-response");
        Generation generation = new Generation(assistantMessage);
        List<Generation> generations = Collections.singletonList(generation);
        ChatResponse response = new ChatResponse(generations);
        Flux<ChatResponse> responseFlux = Flux.just(response);
        Mockito.when(mock.call(Mockito.any(Prompt.class))).thenReturn(response);
        Mockito.when(mock.stream(Mockito.any(Prompt.class))).thenReturn(responseFlux);
        return mock;
    }

    /**
     * Mock Ollama 简单向量存储
     *
     * @return SimpleVectorStore
     */
    @Bean(name = "ollamaSimpleVectorStore")
    @Primary
    public SimpleVectorStore ollamaSimpleVectorStore() {
        SimpleVectorStore mock = Mockito.mock(SimpleVectorStore.class);
        List<Document> emptyDocuments = Collections.emptyList();
        Mockito.when(mock.similaritySearch(Mockito.any(SearchRequest.class))).thenReturn(emptyDocuments);
        return mock;
    }

    /**
     * Mock Ollama PgVector 向量存储
     *
     * @return PgVectorStore
     */
    @Bean(name = "ollamaPgVectorStore")
    @Primary
    public PgVectorStore ollamaPgVectorStore() {
        PgVectorStore mock = Mockito.mock(PgVectorStore.class);
        List<Document> emptyDocuments = Collections.emptyList();
        Mockito.when(mock.similaritySearch(Mockito.any(SearchRequest.class))).thenReturn(emptyDocuments);
        return mock;
    }

    /**
     * Mock ToolCallbackProvider
     * 用于测试环境，避免 MCPServerCSDNJob 依赖注入失败
     *
     * @return ToolCallbackProvider
     */
    @Bean
    @Primary
    public ToolCallbackProvider toolCallbackProvider() {
        ToolCallbackProvider mock = Mockito.mock(ToolCallbackProvider.class);
        ToolCallback toolCallback = Mockito.mock(ToolCallback.class);
        ToolCallback[] callbacks = new ToolCallback[]{toolCallback};
        Mockito.when(mock.getToolCallbacks()).thenReturn(callbacks);
        return mock;
    }

    /**
     * Mock ToolCallingManager
     * 用于测试环境，避免 ChatModel 依赖注入失败
     *
     * @return ToolCallingManager
     */
    @Bean
    @Primary
    public ToolCallingManager toolCallingManager() {
        return Mockito.mock(ToolCallingManager.class);
    }

    /**
     * Mock ModelProviderFactory
     * 避免测试中真实创建外部模型客户端
     *
     * @param openAiChatModel Mock OpenAI ChatModel
     * @return ModelProviderFactory
     */
    @Bean
    @Primary
    public ModelProviderFactory modelProviderFactory(OpenAiChatModel openAiChatModel) {
        ModelProviderFactory mock = Mockito.mock(ModelProviderFactory.class);
        ChatClient chatClient = ChatClient.builder(openAiChatModel).build();
        Mockito.when(mock.createChatClient(Mockito.any(ModelConfig.class))).thenReturn(chatClient);
        Mockito.when(mock.isSupported(Mockito.any(ModelType.class))).thenReturn(true);

        ModelProvider provider = Mockito.mock(ModelProvider.class);
        Mockito.when(provider.createChatClient(Mockito.any(ModelConfig.class))).thenReturn(chatClient);
        Mockito.when(provider.getModelType()).thenReturn(ModelType.OPENAI);
        Mockito.when(provider.isHealthy(Mockito.any(ModelConfig.class))).thenReturn(true);
        Mockito.when(mock.getProvider(Mockito.any(ModelType.class))).thenReturn(provider);
        return mock;
    }

    /**
     * Mock MCPServerCSDNJob
     * 避免测试触发真实外部调用
     *
     * @return MCPServerCSDNJob
     */
    @Bean
    @Primary
    public MCPServerCSDNJob mcpServerCSDNJob() {
        return Mockito.mock(MCPServerCSDNJob.class);
    }
}
