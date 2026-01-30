package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 任务类型选择处理器
 * 当请求携带任务类型时触发分派
 *
 * 设计模式：责任链节点（任务类型优先）
 * 职责：保持任务类型语义优先于默认策略
 * @author xiexu
 */
@Component
@Order(2)
public class TaskTypeSelectionHandler implements ModelSelectionHandler {

    /**
     * 对外暴露 supports 作为调用入口，便于上层复用。
     */
    @Override
    public boolean supports(AICallCommand request) {
        return request.getTaskType() != null;
    }

    /**
     * 对外暴露 select 作为调用入口，便于上层复用。
     */
    @Override
    public ModelSelectionDecision select(AICallCommand request) {
        String taskType = request.getTaskType();
        return ModelSelectionDecision.byTaskType(taskType);
    }
}
