package com.xbk.knowledge.application.provider;

import com.xbk.knowledge.types.enums.ModelType;

/**
 * 模型提供者工厂接口
 * 定义了根据模型类型获取 Provider 的契约
 *
 * 职责：模型调用抽象契约，用于隔离厂商差异
 * @author sxie
 */
public interface ModelProviderFactory {

    /**
     * 根据模型类型获取对应的 Provider
     *
     * 为什么：统一 Provider 路由逻辑
     * 入参：模型类型
     * 出参：ModelProvider 实例
     * @throws IllegalArgumentException 如果模型类型不支持
     */
    ModelProvider getProvider(ModelType modelType);

    /**
     * 检查指定模型类型是否支持
     *
     * 为什么：供上层进行能力探测
     * 入参：模型类型
     * 出参：是否支持
     */
    boolean isSupported(ModelType modelType);
}
