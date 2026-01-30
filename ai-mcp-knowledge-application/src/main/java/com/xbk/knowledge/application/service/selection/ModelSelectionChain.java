package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型选择责任链
 * 按优先级依次匹配并输出选择决策
 *
 * 设计模式：责任链（Chain of Responsibility）
 * 职责：统一选择入口，降低调用方复杂度
 * @author xiexu
 */
@Component
@RequiredArgsConstructor
public class ModelSelectionChain {

    private final List<ModelSelectionHandler> handlers;

    /**
     * 选择模型决策
     *
     * @param request 请求参数
     * @return 选择决策
     */
    public ModelSelectionDecision select(AICallCommand request) {
        for (ModelSelectionHandler handler : handlers) {
            if (handler.supports(request)) {
                return handler.select(request);
            }
        }
        throw new IllegalStateException("模型选择处理器未配置");
    }
}
