package com.xbk.knowledge.application.service.rag;

import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 向量存储服务
 * 负责根据激活的嵌入模型构建向量库，并提供检索/维护能力
 *
 * 职责：向量库访问与 Embedding 适配
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagVectorStoreService {

    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.35;
    private static final int DEFAULT_TOP_K = 5;

    private final ModelConfigAppService modelConfigAppService;

    @Qualifier("pgvectorJdbcTemplate")
    private final JdbcTemplate pgvectorJdbcTemplate;

    @Value("${vector.store.openai.table-name}")
    private String openAiTableName;

    @Value("${vector.store.ollama.table-name}")
    private String ollamaTableName;

    /**
     * 查询知识库标签列表
     *
     * @return 标签列表
     */
    public List<String> listTags() {
        String tableName = resolveActiveVectorTableName();
        if (!StringUtils.hasText(tableName)) {
            return Collections.emptyList();
        }
        String sql = "select distinct metadata->>'knowledge' as rag_tag " +
                "from " + tableName + " " +
                "where metadata ? 'knowledge' " +
                "order by rag_tag";
        return pgvectorJdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 删除指定标签的向量数据
     *
     * @param ragTag 标签
     * @return 删除行数
     */
    public int deleteByTag(String ragTag) {
        String tableName = resolveActiveVectorTableName();
        if (!StringUtils.hasText(tableName)) {
            return 0;
        }
        String sql = "delete from " + tableName + " where metadata->>'knowledge' = ?";
        return pgvectorJdbcTemplate.update(sql, ragTag);
    }

    /**
     * 统计标签向量数量
     *
     * @param ragTag 标签
     * @return 数量
     */
    public long countByTag(String ragTag) {
        String tableName = resolveActiveVectorTableName();
        if (!StringUtils.hasText(tableName)) {
            return 0L;
        }
        String sql = "select count(1) from " + tableName + " where metadata->>'knowledge' = ?";
        Long count = pgvectorJdbcTemplate.queryForObject(sql, Long.class, ragTag);
        return count != null ? count : 0L;
    }

    /**
     * 写入文档到向量库
     *
     * @param documents 文档
     */
    public void saveDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        PgVectorStore vectorStore = buildActiveVectorStore();
        vectorStore.accept(documents);
    }

    /**
     * 相似度检索
     *
     * @param query 查询文本
     * @param ragTags 标签列表
     * @return 文档列表
     */
    public List<Document> similaritySearch(String query, List<String> ragTags) {
        PgVectorStore vectorStore = buildActiveVectorStore();
        String filterExpression = buildFilterExpression(ragTags);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(filterExpression)
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        return documents != null ? documents : Collections.emptyList();
    }

    /**
     * 构建过滤表达式
     */
    private String buildFilterExpression(List<String> ragTags) {
        if (CollectionUtils.isEmpty(ragTags)) {
            return null;
        }
        return ragTags.stream()
                .filter(StringUtils::hasText)
                .map(tag -> "knowledge == '" + tag + "'")
                .collect(Collectors.joining(" || "));
    }

    /**
     * 构建当前激活的向量库
     */
    private PgVectorStore buildActiveVectorStore() {
        ModelConfig embeddingModel = modelConfigAppService.getActiveEmbeddingModel();
        if (embeddingModel == null) {
            throw new IllegalStateException("未配置激活的嵌入模型");
        }
        EmbeddingModel embedding = buildEmbeddingModel(embeddingModel);
        String tableName = resolveVectorTableName(embeddingModel.getModelType());
        if (!StringUtils.hasText(tableName)) {
            throw new IllegalStateException("向量表名未配置");
        }
        return PgVectorStore.builder(pgvectorJdbcTemplate, embedding)
                .vectorTableName(tableName)
                .build();
    }

    /**
     * 构建 EmbeddingModel
     */
    private EmbeddingModel buildEmbeddingModel(ModelConfig modelConfig) {
        ModelType modelType = modelConfig.getModelType();
        String modelName = modelConfig.getModelName();
        if (modelType == ModelType.OPENAI || modelType == ModelType.DEEPSEEK) {
            String baseUrl = normalizeBaseUrl(modelConfig.getBaseUrl());
            String apiKey = modelConfig.getApiKey();
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .embeddingsPath(resolveEmbeddingsPath(modelConfig.getEmbeddingsPath()))
                    .build();
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(modelName)
                    .build();
            return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options);
        }
        if (modelType == ModelType.OLLAMA) {
            String baseUrl = modelConfig.getBaseUrl();
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(baseUrl)
                    .build();
            OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                    .model(modelName)
                    .build();
            return OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(options)
                    .build();
        }
        throw new IllegalArgumentException("当前模型类型不支持作为嵌入模型");
    }

    /**
     * 根据模型类型获取向量表名
     */
    private String resolveVectorTableName(ModelType modelType) {
        if (modelType == ModelType.OLLAMA) {
            return ollamaTableName;
        }
        return openAiTableName;
    }

    /**
     * 获取当前激活模型对应的向量表名
     */
    private String resolveActiveVectorTableName() {
        ModelConfig embeddingModel = modelConfigAppService.getActiveEmbeddingModel();
        if (embeddingModel == null) {
            return null;
        }
        return resolveVectorTableName(embeddingModel.getModelType());
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

    /**
     * 规范化 embeddingsPath，保证以 '/' 开头并提供默认值。
     */
    private String resolveEmbeddingsPath(String embeddingsPath) {
        String resolved = StringUtils.hasText(embeddingsPath) ? embeddingsPath.trim() : "/v1/embeddings";
        return resolved.startsWith("/") ? resolved : "/" + resolved;
    }
}
