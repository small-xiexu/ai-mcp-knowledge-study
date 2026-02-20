package com.xbk.knowledge.application.service.selection.handler;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selection.chain.ModelSelectionChainArmory;

/**
 * 模型选择处理器
 * 负责判断是否适用并产出选择决策
 *
 * 设计模式：责任链节点（Chain of Responsibility）
 * 职责：解耦选择逻辑，便于按优先级扩展
 * @author sxie
 */
public interface ModelSelectionHandler extends ModelSelectionChainArmory {

    /**
     * 是否支持当前请求
     *
     * @param request 请求参数
     * @return 是否支持
     */
    boolean supports(AICallCommand request);

    /**
     * 生成选择决策
     *
     * @param request 请求参数
     * @return 选择决策
     */
    ModelSelectionDecision select(AICallCommand request);
}
