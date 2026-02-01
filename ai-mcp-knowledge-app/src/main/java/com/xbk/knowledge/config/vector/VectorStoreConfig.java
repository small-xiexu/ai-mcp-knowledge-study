package com.xbk.knowledge.config.vector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.util.StringUtils;

/**
 * 向量存储配置
 * 手工装配多向量库，避免自动配置在多 EmbeddingModel 场景下冲突
 *
 * @author xiexu
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfig {

    private final JdbcTemplate pgvectorJdbcTemplate;
    private final EmbeddingModel openAiEmbeddingModel;
    private final EmbeddingModel ollamaEmbeddingModel;
    private final VectorStoreProperties vectorStoreProperties;

    public VectorStoreConfig(
            @Qualifier("pgvectorJdbcTemplate") JdbcTemplate pgvectorJdbcTemplate,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel openAiEmbeddingModel,
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel,
            VectorStoreProperties vectorStoreProperties) {
        this.pgvectorJdbcTemplate = pgvectorJdbcTemplate;
        this.openAiEmbeddingModel = openAiEmbeddingModel;
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
        this.vectorStoreProperties = vectorStoreProperties;
    }

    /**
     * OpenAI 向量存储
     *
     * @return PgVectorStore
     */
    @Bean(name = "openAiPgVectorStore")
    public PgVectorStore openAiPgVectorStore() {
        String tableName = vectorStoreProperties.getOpenai().getTableName();
        return buildPgVectorStore(openAiEmbeddingModel, tableName);
    }

    /**
     * Ollama 向量存储
     *
     * @return PgVectorStore
     */
    @Bean(name = "ollamaPgVectorStore")
    public PgVectorStore ollamaPgVectorStore() {
        String tableName = vectorStoreProperties.getOllama().getTableName();
        return buildPgVectorStore(ollamaEmbeddingModel, tableName);
    }

    /**
     * 构建 PgVectorStore
     *
     * @param embeddingModel 嵌入模型
     * @param tableName      向量表名
     * @return PgVectorStore
     */
    private PgVectorStore buildPgVectorStore(EmbeddingModel embeddingModel, String tableName) {
        if (!StringUtils.hasText(tableName)) {
            throw new IllegalStateException("向量表名未配置");
        }
        return PgVectorStore.builder(pgvectorJdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .build();
    }
}
