package com.xbk.knowledge.infrastructure.provider;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型提供者工厂实现
 * 根据模型类型返回对应的 Provider
 *
 * 职责：模型调用实现，用于适配具体厂商 SDK
 * @author xiexu
 */
@Slf4j
@Component
public class ModelProviderFactoryImpl implements ModelProviderFactory {

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
    public ModelProviderFactoryImpl(List<ModelProvider> providers) {
        // 将 Provider 列表转换为 Map，以 ModelType 为 key
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        ModelProvider::getModelType,
                        Function.identity()
                ));
        log.info("初始化 ModelProviderFactoryImpl，已注册 {} 个模型提供者", providerMap.size());
    }

    /**
     * 根据模型类型获取对应的 Provider
     *
     * @param modelType 模型类型
     * @return ModelProvider 实例
     * @throws IllegalArgumentException 如果模型类型不支持
     */
    @Override
    public ModelProvider getProvider(ModelType modelType) {
        ModelProvider provider = providerMap.get(modelType);
        if (provider == null) {
            // 直接抛错以便快速暴露配置问题，避免静默降级造成错误选择
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
    @Override
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
    @Override
    public boolean isSupported(ModelType modelType) {
        return providerMap.containsKey(modelType);
    }
}
