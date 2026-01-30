package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.domain.model.entity.ModelConfig;

import java.util.List;

/**
 * 默认降级计划
 * 按顺序遍历候选模型，首个为主模型
 *
 * 设计模式：迭代器实现（Iterator Implementation）
 * 职责：应用层默认迭代器，用于隐藏主/备判断逻辑
 * @author xiexu
 */
public class DefaultFailoverPlan implements FailoverPlan {

    private final List<ModelConfig> candidates;
    private final String primaryName;
    private final int fallbackCount;
    private int index;

    /**
     * 对外暴露 DefaultFailoverPlan 作为调用入口，便于上层复用。
     */
    public DefaultFailoverPlan(List<ModelConfig> candidates, ModelConfig primary, List<ModelConfig> fallbacks) {
        this.candidates = candidates;
        this
                .primaryName = primary != null ? primary
                .getModelName() : "UNKNOWN";
        this
                .fallbackCount = fallbacks != null ? fallbacks
                .size() : 0;
        this.index = 0;
    }

    /**
     * 对外暴露 hasNext 作为调用入口，便于上层复用。
     */
    @Override
    public boolean hasNext() {
        return candidates != null && index < candidates.size();
    }

    /**
     * 对外暴露 next 作为调用入口，便于上层复用。
     */
    @Override
    public FailoverCandidate next() {
        ModelConfig model = candidates.get(index);
        /**
         * 通过索引判断是否为备用模型：
         * index == 0 代表主模型，其余为降级候选。
         */
        boolean isFallback = index > 0;
        index++;
        return FailoverCandidate.builder()
                .model(model)
                .fallback(isFallback)
                .build();
    }

    /**
     * 对外暴露 getPrimaryName 作为调用入口，便于上层复用。
     */
    @Override
    public String getPrimaryName() {
        return primaryName;
    }

    /**
     * 对外暴露 getFallbackCount 作为调用入口，便于上层复用。
     */
    @Override
    public int getFallbackCount() {
        return fallbackCount;
    }
}
