package com.xbk.knowledge.infrastructure.provider.factory;

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
import java.util.stream.Collector;
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
        /**
         * 将 Provider 列表转换为 Map，以 ModelType 为 key，
         * 这样可以在运行期用 O(1) 复杂度快速定位具体 Provider，避免线性扫描影响调用路径性能。
         */
        Function<ModelProvider, ModelType> typeMapper = ModelProvider::getModelType;
        Function<ModelProvider, ModelProvider> identityMapper = Function.identity();
        Collector<ModelProvider, ?, Map<ModelType, ModelProvider>> collector = Collectors.toMap(
                typeMapper,
                identityMapper
        );
        /**
         * 通过收集器一次性构建映射，集中完成转换逻辑，
         * 便于在构造阶段统一校验并保证工厂初始化时映射关系稳定可读。
         */
        this.providerMap = providers
                .stream()
                .collect(collector);
        /**
         * 记录已注册 Provider 数量，便于启动时快速确认模型适配是否完整，
         * 避免运行时才暴露缺失配置问题。
         */
        int providerCount = providerMap.size();
        log.info("初始化 ModelProviderFactoryImpl，已注册 {} 个模型提供者", providerCount);
    }

    /**
     * 根据模型类型获取对应的 Provider
     *
     * @param modelType 模型类型
     * @return ModelProvider 实例
     * @throws IllegalArgumentException 如果模型类型不支持
     *
     * 为什么：统一 Provider 路由逻辑，避免上层分散判断
     */
    @Override
    public ModelProvider getProvider(ModelType modelType) {
        /**
         * 根据模型类型直接命中对应 Provider，
         * 将类型选择逻辑集中在工厂内，避免上层出现分散的类型判断。
         */
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
     *
     * 为什么：统一在工厂层完成模型选择与创建
     */
    @Override
    public ChatClient createChatClient(ModelConfig config) {
        /**
         * 入口处只接受完整的模型配置，避免调用方自行拆分配置字段，
         * 将配置解析与路由逻辑统一放在工厂层，确保职责边界一致。
         */
        ModelType modelType = config.getModelType();
        /**
         * 依据模型类型获取匹配的 Provider，
         * 这样可以在新增模型类型时只扩展 Provider，而不需要改动调用方。
         */
        ModelProvider provider = getProvider(modelType);
        /**
         * 由 Provider 负责具体 ChatClient 的创建与参数适配，
         * 保持工厂只负责“选择”，不承担具体厂商 SDK 的构建细节。
         */
        return provider.createChatClient(config);
    }

    /**
     * 检查指定模型类型是否支持
     *
     * @param modelType 模型类型
     * @return 是否支持
     *
     * 为什么：供上层在调用前做能力探测
     */
    @Override
    public boolean isSupported(ModelType modelType) {
        /**
         * 通过 Map 判定是否支持对应模型类型，
         * 便于上层在执行前进行能力探测或降级决策。
         */
        return providerMap.containsKey(modelType);
    }
}
