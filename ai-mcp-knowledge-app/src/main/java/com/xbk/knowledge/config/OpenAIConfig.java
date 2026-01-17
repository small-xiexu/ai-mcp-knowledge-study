package com.xbk.knowledge.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OpenAI 向量存储配置类
 * 配置 OpenAI 嵌入模型与向量存储（SimpleVectorStore 和 PgVectorStore）
 *
 * @author xiexu
 */
@Configuration
public class OpenAIConfig {

    /**
     * 文本分割器
     * 用于将长文本按 Token 数量切分为适合嵌入模型处理的小块
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }


    /**
     * OpenAI API 客户端
     * 从配置文件读取 baseUrl 和 apiKey，支持自定义代理地址
     */
    @Bean
    public OpenAiApi openAiApi(@Value("${spring.ai.openai.base-url}") String baseUrl, @Value("${spring.ai.openai.api-key}") String apikey) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apikey)
                .build();
    }

    /**
     * 基于内存的简单向量存储
     * 适用于开发测试环境，数据不持久化
     */
    @Bean("openAiSimpleVectorStore")
    public SimpleVectorStore vectorStore(OpenAiApi openAiApi) {
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi);
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 基于 PostgreSQL 的向量存储（生产环境推荐）
     * 使用 pgvector 扩展实现向量相似度检索，向量维度为 1536（OpenAI text-embedding-ada-002）
     *
     * <pre>
     * -- 建表 SQL：
     * CREATE TABLE public.vector_store_openai (
     *     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     *     content TEXT NOT NULL,
     *     metadata JSONB,
     *     embedding VECTOR(1536)
     * );
     * </pre>
     */
    @Bean("openAiPgVectorStore")
    public PgVectorStore pgVectorStore(OpenAiApi openAiApi,
                                       JdbcTemplate jdbcTemplate,
                                       @Value("${vector.store.openai.table-name}") String tableName) {
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .build();
    }

}
