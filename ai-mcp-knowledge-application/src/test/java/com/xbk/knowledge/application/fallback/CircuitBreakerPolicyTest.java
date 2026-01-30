package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        ModelCallOutcome outcome = policy.apply(context, Mockito.mock(ModelCallPolicyChain.class));

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

        ModelCallPolicyChain chain = Mockito.mock(ModelCallPolicyChain.class);
        ModelCallOutcome success = ModelCallOutcome.success(AICallResult.builder().success(true).build());
        when(chain.proceed(any(ModelCallContext.class))).thenReturn(success);

        CircuitBreakerPolicy policy = new CircuitBreakerPolicy(circuitBreaker);
        ModelConfig model = ModelConfig.builder().id(1L).modelName("m1").build();
        ModelCallContext context = ModelCallContext.builder().model(model).build();

        policy.apply(context, chain);

        verify(circuitBreaker).recordSuccess(1L);
    }
}
