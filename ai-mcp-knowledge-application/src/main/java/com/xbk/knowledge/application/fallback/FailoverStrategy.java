package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.domain.model.entity.ModelConfig;

import java.util.List;

/**
 * 降级策略
 * 用于定义模型候选顺序与切换规则
 *
 * 设计模式：策略（Strategy）
 * 职责：应用层策略接口，用于扩展降级规则并隔离排序逻辑
 * @author xiexu
 */
public interface FailoverStrategy {

    /**
     * 构建候选模型列表
     *
     * @param primary   主模型
     * @param fallbacks 备用模型
     * @param request   请求参数
     * @return 候选模型列表
     */
    List<ModelConfig> orderCandidates(ModelConfig primary, List<ModelConfig> fallbacks, AICallCommand request);
}
