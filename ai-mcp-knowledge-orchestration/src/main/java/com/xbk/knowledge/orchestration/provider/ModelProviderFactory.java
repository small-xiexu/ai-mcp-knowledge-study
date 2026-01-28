package com.xbk.knowledge.orchestration.provider;

import com.xbk.knowledge.orchestration.config.ChatClientEnhancer;
import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ModelType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 模型提供者工厂
 * 根据模型类型返回对应的 Provider
 *
 * @author xiexu
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelProviderFactory {

    /**
     * Provider 列表由 Spring 注入
     * 通过初始化阶段集中构建映射，保证后续查找稳定
     */
    private final List<ModelProvider> providers;

    /**
     * ChatClient 增强器
     * 统一注入工具与 Advisor，避免重复装配
     */
    private final ChatClientEnhancer chatClientEnhancer;

    /**
     * 所有模型提供者的映射
     * Key: ModelType, Value: ModelProvider
     */
    private final Map<ModelType, ModelProvider> providerMap = new EnumMap<>(ModelType.class);

    /**
     * 初始化 Provider 映射
     * 统一在启动阶段完成映射构建，避免运行时重复计算
     */
    @PostConstruct
    private void initProviderMap() {
        for (var provider : providers) {
            providerMap.put(provider.getModelType(), provider);
        }
        log.info("初始化 ModelProviderFactory，已注册 {} 个模型提供者", providerMap.size());
    }

    /**
     * 根据模型类型获取对应的 Provider
     *
     * @param modelType 模型类型
     * @return ModelProvider 实例
     * @throws IllegalArgumentException 如果模型类型不支持
     */
    public ModelProvider getProvider(ModelType modelType) {
        ModelProvider provider = providerMap.get(modelType);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的模型类型: " + modelType);
        }
        return provider;
    }

    /**
     * 根据模型配置创建 ChatClient
     *
     * @param config 模型配置
     * @return ChatClient 实例
     */
    public ChatClient createChatClient(ModelConfig config) {
        var provider = getProvider(config.getModelType());
        var chatModel = provider.createChatModel(config);
        return chatClientEnhancer.enhance(chatModel);
    }

    /**
     * 检查指定模型类型是否支持
     *
     * @param modelType 模型类型
     * @return 是否支持
     */
    public boolean isSupported(ModelType modelType) {
        return providerMap.containsKey(modelType);
    }
}
