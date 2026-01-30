package com.xbk.knowledge.test;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

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
        List<Generation> generations = List.of(generation);
        ChatResponse response = new ChatResponse(generations);
        Prompt anyPrompt = Mockito.any(Prompt.class);
        Mockito
                .doReturn(response)
                .when(mock)
                .call(anyPrompt);
        Flux<ChatResponse> responseFlux = Flux.just(response);
        Mockito
                .doReturn(responseFlux)
                .when(mock)
                .stream(anyPrompt);
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
        SearchRequest anySearchRequest = Mockito.any(SearchRequest.class);
        List<Document> emptyDocuments = List.of();
        Mockito
                .doReturn(emptyDocuments)
                .when(mock)
                .similaritySearch(anySearchRequest);
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
        SearchRequest anySearchRequest = Mockito.any(SearchRequest.class);
        List<Document> emptyDocuments = List.of();
        Mockito
                .doReturn(emptyDocuments)
                .when(mock)
                .similaritySearch(anySearchRequest);
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
     * Mock ToolCallbackProvider
     * 用于测试环境，避免 MCPServerCSDNJob 依赖注入失败
     *
     * 注意：已移除此 Mock Bean，改为使用真实的 MCP ToolCallbackProvider
     * 这样可以在测试中正常使用 MCP 工具
     *
     * @return ToolCallbackProvider
     */
    // @Bean
    // @Primary
    // public ToolCallbackProvider toolCallbackProvider() {
    //     var mock = Mockito.mock(ToolCallbackProvider.class);
    //     // 配置 mock 返回空数组，避免 NullPointerException
    //     Mockito.when(mock.getToolCallbacks()).thenReturn(new org.springframework.ai.tool.ToolCallback[0]);
    //     return mock;
    // }

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
}
