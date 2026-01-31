package com.xbk.knowledge.application.service.selection.handler;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selection.chain.AbstractModelSelectionHandler;
import org.springframework.stereotype.Component;

/**
 * 任务类型选择处理器
 * 当请求携带任务类型时触发分派
 *
 * 设计模式：责任链节点（任务类型优先）
 * 职责：命中任务类型直接处理，未命中则交给下一个节点
 * @author xiexu
 */
@Component
public class TaskTypeSelectionHandler extends AbstractModelSelectionHandler {

    /**
     * 对外暴露 supports 作为判断入口，未命中则交由下一个节点处理。
     */
    @Override
    public boolean supports(AICallCommand request) {
        return request.getTaskType() != null;
    }

    /**
     * 处理任务类型，未命中时调用 next 继续责任链。
     */
    @Override
    protected ModelSelectionDecision doSelect(AICallCommand request) {
        if (!supports(request)) {
            return next().select(request);
        }
        String taskType = request.getTaskType();
        return ModelSelectionDecision.byTaskType(taskType);
    }
}
