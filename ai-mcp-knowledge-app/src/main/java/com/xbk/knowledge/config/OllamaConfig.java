package com.xbk.knowledge.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Ollama 向量存储配置类
 * 配置 Ollama 嵌入模型与向量存储（SimpleVectorStore 和 PgVectorStore）
 *
 * @author xiexu
 */
@Configuration
public class OllamaConfig {

    /**
     * 自定义 OllamaApi，配置正确的请求头
     * 解决京东云 DeepSeek API 调用时 "missing request body" 问题
     */
    @Bean
    @Primary
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return execution.execute(request, body);
                });
        return new OllamaApi(baseUrl, restClientBuilder, WebClient.builder());
    }

    /**
     * 自定义 OllamaChatModel
     */
    @Bean
    @Primary
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .build();
    }

    /**
     * 基于内存的简单向量存储
     * 使用 Ollama 本地嵌入模型（nomic-embed-text），适用于开发测试环境
     */
    @Bean("ollamaSimpleVectorStore")
    public SimpleVectorStore vectorStore(OllamaApi ollamaApi) {
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel
                .builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder().model("nomic-embed-text").build())
                .build();
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 基于 PostgreSQL 的向量存储（生产环境推荐）
     * 使用 pgvector 扩展实现向量相似度检索，向量维度为 768（nomic-embed-text）
     *
     * <pre>
     * -- 建表 SQL：
     * CREATE TABLE public.vector_store_ollama_deepseek (
     *     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     *     content TEXT NOT NULL,
     *     metadata JSONB,
     *     embedding VECTOR(768)
     * );
     * </pre>
     */
    @Bean("ollamaPgVectorStore")
    public PgVectorStore pgVectorStore(OllamaApi ollamaApi,
                                       JdbcTemplate jdbcTemplate,
                                       @Value("${vector.store.ollama.table-name}") String tableName) {
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel
                .builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder().model("nomic-embed-text").build())
                .build();
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .build();
    }

}
