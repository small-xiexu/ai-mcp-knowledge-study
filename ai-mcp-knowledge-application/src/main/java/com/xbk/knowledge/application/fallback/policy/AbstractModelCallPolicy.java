package com.xbk.knowledge.application.fallback.policy;

/**
 * 模型调用策略抽象实现
 * 维护责任链指针，便于子类显式触发下一个节点
 *
 * @author xiexu
 */
public abstract class AbstractModelCallPolicy implements ModelCallPolicy {

    /**
     * 责任链指针，指向下一个策略节点
     */
    private ModelCallPolicy next;

    /**
     * next。
     *
     * @return 返回结果
     */
    @Override
    public ModelCallPolicy next() {
        return next;
    }

    /**
     * appendNext。
     *
     * @param next 参数
     * @return 返回结果
     */
    @Override
    public ModelCallPolicy appendNext(ModelCallPolicy next) {
        this.next = next;
        return next;
    }
}
