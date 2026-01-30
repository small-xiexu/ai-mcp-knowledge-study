package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 熔断策略
 * 在模型不健康时快速失败，避免持续消耗
 *
 * 设计模式：责任链节点（Circuit Breaker Interceptor）
 * 职责：责任链中的稳定性节点，用于隔离不健康模型
 * @author xiexu
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class CircuitBreakerPolicy implements ModelCallPolicy {

    private final CircuitBreaker circuitBreaker;

    /**
     * 对外暴露 apply 作为调用入口，便于上层复用。
     */
    @Override
    public ModelCallOutcome apply(ModelCallContext context, ModelCallPolicyChain chain) {
        ModelConfig model = context.getModel();
        Long modelId = model.getId();

        if (circuitBreaker.isOpen(modelId)) {
            String modelName = model.getModelName();
            AICallResult skippedResult = AICallResult.builder()
                    .success(false)
                    .errorMessage("模型已熔断")
                    .modelUsed(modelName)
                    .fallback(false)
                    .retryCount(0)
                    .build();
            return ModelCallOutcome.skipped(skippedResult);
        }

        ModelCallOutcome outcome = chain.proceed(context);
        if (outcome.isSuccess()) {
            circuitBreaker.recordSuccess(modelId);
            return outcome;
        }

        if (!outcome.isSkipped()) {
            circuitBreaker.recordFailure(modelId);
        }
        return outcome;
    }
}
