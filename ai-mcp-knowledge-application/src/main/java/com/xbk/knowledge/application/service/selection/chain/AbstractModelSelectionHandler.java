package com.xbk.knowledge.application.service.selection.chain;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selection.handler.ModelSelectionHandler;

/**
 * 模型选择责任链抽象处理器
 * 维护责任链指针，便于子类显式触发下一个节点
 *
 * @author xiexu
 */
public abstract class AbstractModelSelectionHandler implements ModelSelectionHandler {

    /**
     * 责任链指针，指向下一个节点
     */
    private ModelSelectionHandler next;

    /**
     * select。
     *
     * @param request 参数
     * @return 返回结果
     */
    @Override
    public ModelSelectionDecision select(AICallCommand request) {
        return doSelect(request);
    }

    /**
     * 子类实现具体选择逻辑
     *
     * @param request 请求参数
     * @return 选择决策
     */
    protected abstract ModelSelectionDecision doSelect(AICallCommand request);

    /**
     * 获取下一个处理器
     *
     * @return 下一个处理器
     */
    @Override
    public ModelSelectionHandler next() {
        return next;
    }

    /**
     * 追加下一个处理器
     *
     * @param next 下一个处理器
     * @return 下一个处理器
     */
    @Override
    public ModelSelectionHandler appendNext(ModelSelectionHandler next) {
        this.next = next;
        return next;
    }
}
