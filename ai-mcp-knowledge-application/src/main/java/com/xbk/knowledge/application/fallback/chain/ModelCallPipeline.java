package com.xbk.knowledge.application.fallback.chain;

import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.core.ModelCallOutcome;
import com.xbk.knowledge.application.fallback.executor.ModelCallExecutor;
import com.xbk.knowledge.application.fallback.policy.AbstractModelCallPolicy;
import com.xbk.knowledge.application.fallback.policy.ModelCallPolicy;
import com.xbk.knowledge.application.model.dto.AICallResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型调用管道
 * 统一组织策略拦截器与执行器，采用责任链叠加横切能力
 *
 * 设计模式：责任链（Chain of Responsibility）
 * 职责：应用层调用入口，用于组合调用能力并降低耦合
 * @author sxie
 */
@Component
public class ModelCallPipeline {

    private final ModelCallPolicy chainHead;

    /**
     * 通过构造函数装配责任链，确保链路顺序稳定可读。
     *
     * @param policies 策略列表
     * @param executor 最终执行器
     */
    @Autowired
    public ModelCallPipeline(List<ModelCallPolicy> policies, ModelCallExecutor executor) {
        ModelCallPolicy terminalPolicy = new TerminalModelCallPolicy(executor);
        if (policies == null || policies.isEmpty()) {
            this.chainHead = terminalPolicy;
            return;
        }

        ModelCallPolicy head = policies.get(0);
        ModelCallPolicy current = head;
        for (int i = 1; i < policies.size(); i++) {
            current = current.appendNext(policies.get(i));
        }
        current.appendNext(terminalPolicy);
        this.chainHead = head;
    }

    /**
     * 执行调用
     *
     * @param context 调用上下文
     * @return 调用结果
     */
    public ModelCallOutcome execute(ModelCallContext context) {
        return chainHead.apply(context);
    }

    /**
     * 终止策略：负责调用执行器并返回最终结果
     */
    private static class TerminalModelCallPolicy extends AbstractModelCallPolicy {
        private final ModelCallExecutor executor;

        private TerminalModelCallPolicy(ModelCallExecutor executor) {
            this.executor = executor;
        }

        /**
         * apply。
         *
         * @param context 参数
         * @return 返回结果
         */
        @Override
        public ModelCallOutcome apply(ModelCallContext context) {
            AICallResult result = executor.execute(context);
            Boolean success = result.getSuccess();
            if (Boolean.TRUE.equals(success)) {
                return ModelCallOutcome.success(result);
            }
            return ModelCallOutcome.failed(result);
        }
    }
}
