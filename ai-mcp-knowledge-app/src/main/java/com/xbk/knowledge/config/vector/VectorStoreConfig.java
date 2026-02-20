package com.xbk.knowledge.config.vector;

import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.types.enums.ModelType;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 向量存储配置
 * 手工装配多向量库，避免自动配置在多 EmbeddingModel 场景下冲突
 *
 * @author sxie
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfig {

    private final JdbcTemplate pgvectorJdbcTemplate;
    private final ModelConfigAppService modelConfigAppService;
    private final VectorStoreProperties vectorStoreProperties;

    public VectorStoreConfig(
            @Qualifier("pgvectorJdbcTemplate") JdbcTemplate pgvectorJdbcTemplate,
            ModelConfigAppService modelConfigAppService,
            VectorStoreProperties vectorStoreProperties) {
        this.pgvectorJdbcTemplate = pgvectorJdbcTemplate;
        this.modelConfigAppService = modelConfigAppService;
        this.vectorStoreProperties = vectorStoreProperties;
    }

    /**
     * OpenAI 向量存储
     *
     * @return PgVectorStore
     */
    @Bean(name = "openAiPgVectorStore")
    @Lazy
    public PgVectorStore openAiPgVectorStore() {
        ModelConfig embeddingModel = resolveEmbeddingModel(ModelType.OPENAI, ModelType.DEEPSEEK);
        String tableName = vectorStoreProperties.getOpenai().getTableName();
        EmbeddingModel embedding = buildEmbeddingModel(embeddingModel);
        return buildPgVectorStore(embedding, tableName);
    }

    /**
     * Ollama 向量存储
     *
     * @return PgVectorStore
     */
    @Bean(name = "ollamaPgVectorStore")
    @Lazy
    public PgVectorStore ollamaPgVectorStore() {
        ModelConfig embeddingModel = resolveEmbeddingModel(ModelType.OLLAMA);
        String tableName = vectorStoreProperties.getOllama().getTableName();
        EmbeddingModel embedding = buildEmbeddingModel(embeddingModel);
        return buildPgVectorStore(embedding, tableName);
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

    /**
     * 根据模型类型解析启用的嵌入模型
     * 统一从数据库配置加载，避免依赖 Spring AI 自动配置
     */
    private ModelConfig resolveEmbeddingModel(ModelType... modelTypes) {
        List<ModelConfig> enabledModels = modelConfigAppService.queryEnabledModels(new EnabledQuery(true));
        if (CollectionUtils.isEmpty(enabledModels)) {
            throw new IllegalStateException("未配置启用的嵌入模型");
        }
        List<ModelType> supportedTypes = Arrays.asList(modelTypes);
        ModelConfig selected = enabledModels.stream()
                .filter(model -> model != null && model.getModelType() != null)
                .filter(model -> supportedTypes.contains(model.getModelType()))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            throw new IllegalStateException("未配置指定类型的嵌入模型");
        }
        return selected;
    }

    /**
     * 构建 EmbeddingModel
     */
    private EmbeddingModel buildEmbeddingModel(ModelConfig modelConfig) {
        if (modelConfig == null || modelConfig.getModelType() == null) {
            throw new IllegalArgumentException("嵌入模型配置无效");
        }
        ModelType modelType = modelConfig.getModelType();
        if (modelType == ModelType.OPENAI || modelType == ModelType.DEEPSEEK) {
            String baseUrl = normalizeBaseUrl(modelConfig.getBaseUrl());
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(modelConfig.getApiKey())
                    .build();
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(modelConfig.getModelName())
                    .build();
            return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options);
        }
        if (modelType == ModelType.OLLAMA) {
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(modelConfig.getBaseUrl())
                    .build();
            OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                    .model(modelConfig.getModelName())
                    .build();
            return OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(options)
                    .build();
        }
        throw new IllegalArgumentException("当前模型类型不支持作为嵌入模型");
    }

    /**
     * 规范化 baseUrl
     * 避免 /v1 或 /v1/embeddings 后缀导致重复拼接
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1/embeddings")) {
            normalized = normalized.substring(0, normalized.length() - "/v1/embeddings".length());
        } else if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - "/v1".length());
        }
        return normalized;
    }
}
