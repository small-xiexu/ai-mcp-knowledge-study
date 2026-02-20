package com.xbk.knowledge.application.fallback.policy;

import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.fallback.core.CircuitBreaker;
import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.core.ModelCallOutcome;
import com.xbk.knowledge.application.fallback.policy.AbstractModelCallPolicy;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证熔断策略在不同状态下的行为，避免异常模型重复调用。
 *
 * @author xiexu
 */
public class CircuitBreakerPolicyTest {

    /**
     * 对外暴露 shouldSkipWhenCircuitOpen 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSkipWhenCircuitOpen() {
        CircuitBreaker circuitBreaker = Mockito.mock(CircuitBreaker.class);
        when(circuitBreaker.isOpen(1L)).thenReturn(true);

        CircuitBreakerPolicy policy = new CircuitBreakerPolicy(circuitBreaker);
        ModelConfig model = ModelConfig.builder().id(1L).modelName("m1").build();
        ModelCallContext context = ModelCallContext.builder().model(model).build();
        policy.appendNext(new StubPolicy(ModelCallOutcome.skipped(AICallResult.builder().success(false).build())));
        ModelCallOutcome outcome = policy.apply(context);

        assertEquals(ModelCallOutcome.Status.SKIPPED, outcome.getStatus());
        verify(circuitBreaker).isOpen(1L);
    }

    /**
     * 对外暴露 shouldRecordSuccessWhenOutcomeSuccess 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRecordSuccessWhenOutcomeSuccess() {
        CircuitBreaker circuitBreaker = Mockito.mock(CircuitBreaker.class);
        when(circuitBreaker.isOpen(1L)).thenReturn(false);

        CircuitBreakerPolicy policy = new CircuitBreakerPolicy(circuitBreaker);
        ModelCallOutcome success = ModelCallOutcome.success(AICallResult.builder().success(true).build());
        policy.appendNext(new StubPolicy(success));
        ModelConfig model = ModelConfig.builder().id(1L).modelName("m1").build();
        ModelCallContext context = ModelCallContext.builder().model(model).build();

        policy.apply(context);

        verify(circuitBreaker).recordSuccess(1L);
    }

    private static class StubPolicy extends AbstractModelCallPolicy {
        private final ModelCallOutcome outcome;

        private StubPolicy(ModelCallOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public ModelCallOutcome apply(ModelCallContext context) {
            return outcome;
        }
    }
}
