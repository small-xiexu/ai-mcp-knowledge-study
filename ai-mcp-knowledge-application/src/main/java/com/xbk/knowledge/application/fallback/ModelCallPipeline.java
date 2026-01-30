package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型调用管道
 * 统一组织策略拦截器与执行器，采用责任链叠加横切能力
 *
 * 设计模式：责任链（Chain of Responsibility）
 * 职责：应用层调用入口，用于组合调用能力并降低耦合
 * @author xiexu
 */
@Component
@RequiredArgsConstructor
public class ModelCallPipeline {

    private final List<ModelCallPolicy> policies;
    private final ModelCallExecutor executor;

    /**
     * 执行调用
     *
     * @param context 调用上下文
     * @return 调用结果
     */
    public ModelCallOutcome execute(ModelCallContext context) {
        return new DefaultModelCallPolicyChain(policies, executor)
                .proceed(context);
    }

    /**
     * 默认策略链实现
     */
    private static class DefaultModelCallPolicyChain implements ModelCallPolicyChain {
        private final List<ModelCallPolicy> policies;
        private final ModelCallExecutor executor;
        private int index;

        private DefaultModelCallPolicyChain(List<ModelCallPolicy> policies, ModelCallExecutor executor) {
            this.policies = policies;
            this.executor = executor;
            this.index = 0;
        }

        @Override
        public ModelCallOutcome proceed(ModelCallContext context) {
            if (index < policies.size()) {
                int policyIndex = index;
                index++;
                ModelCallPolicy policy = policies.get(policyIndex);
                return policy.apply(context, this);
            }

            AICallResult result = executor.execute(context);
            Boolean success = result.getSuccess();
            if (Boolean.TRUE.equals(success)) {
                return ModelCallOutcome.success(result);
            }
            return ModelCallOutcome.failed(result);
        }
    }
}
