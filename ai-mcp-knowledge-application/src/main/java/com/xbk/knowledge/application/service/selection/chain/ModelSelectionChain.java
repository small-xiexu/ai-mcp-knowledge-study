package com.xbk.knowledge.application.service.selection.chain;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selection.handler.ModelSelectionHandler;
import lombok.RequiredArgsConstructor;

/**
 * 模型选择责任链
 * 责任链头节点负责驱动后续节点执行
 *
 * 设计模式：责任链（Chain of Responsibility）
 * 职责：统一选择入口，降低调用方复杂度
 * @author sxie
 */
@RequiredArgsConstructor
public class ModelSelectionChain {

    private final ModelSelectionHandler chainHead;

    /**
     * 选择模型决策
     *
     * @param request 请求参数
     * @return 选择决策
     */
    public ModelSelectionDecision select(AICallCommand request) {
        if (chainHead == null) {
            throw new IllegalStateException("模型选择处理器未配置");
        }
        return chainHead.select(request);
    }
}
