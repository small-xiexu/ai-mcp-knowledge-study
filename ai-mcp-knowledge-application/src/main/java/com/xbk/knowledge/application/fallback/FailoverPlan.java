package com.xbk.knowledge.application.fallback;

/**
 * 降级计划
 * 负责输出候选模型迭代顺序
 *
 * 设计模式：迭代器（Iterator）
 * 职责：应用层迭代器，用于隐藏降级顺序细节
 * @author xiexu
 */
public interface FailoverPlan {

    /**
     * 是否还有候选模型
     *
     * @return 是否还有候选模型
     */
    boolean hasNext();

    /**
     * 获取下一个候选模型
     *
     * @return 候选模型
     */
    FailoverCandidate next();

    /**
     * 主模型名称
     *
     * @return 主模型名称
     */
    String getPrimaryName();

    /**
     * 备用模型数量
     *
     * @return 备用模型数量
     */
    int getFallbackCount();
}
