package com.xbk.knowledge.orchestration.provider;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型提供者工厂
 * 根据模型类型返回对应的 Provider
 *
 * @author xiexu
 */
@Component
@Slf4j
public class ModelProviderFactory {

    /**
     * 所有模型提供者的映射
     * Key: ModelType, Value: ModelProvider
     */
    private final Map<ModelType, ModelProvider> providerMap;

    /**
     * 构造函数注入所有 ModelProvider 实现
     *
     * @param providers 所有 ModelProvider 实现的列表
     */
    @Autowired
    public ModelProviderFactory(List<ModelProvider> providers) {
        // 将 Provider 列表转换为 Map，以 ModelType 为 key
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        ModelProvider::getModelType,
                        Function.identity()
                ));
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
        ModelProvider provider = getProvider(config.getModelType());
        return provider.createChatClient(config);
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
