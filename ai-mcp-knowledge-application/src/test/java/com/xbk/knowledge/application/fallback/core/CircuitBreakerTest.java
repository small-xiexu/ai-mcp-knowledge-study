package com.xbk.knowledge.application.fallback.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证熔断器阈值与恢复逻辑，避免异常模型持续占用资源。
 *
 * @author xiexu
 */
public class CircuitBreakerTest {

    /**
     * 对外暴露 shouldOpenAfterFailures 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldOpenAfterFailures() {
        CircuitBreaker breaker = new CircuitBreaker();
        Long modelId = 1L;

        breaker.recordFailure(modelId);
        breaker.recordFailure(modelId);
        breaker.recordFailure(modelId);

        assertTrue(breaker.isOpen(modelId));
    }

    /**
     * 对外暴露 shouldRecoverAfterTimeout 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRecoverAfterTimeout() {
        CircuitBreaker breaker = new CircuitBreaker();
        Long modelId = 2L;

        breaker.recordFailure(modelId);
        breaker.recordFailure(modelId);
        breaker.recordFailure(modelId);
        CircuitBreaker.CircuitState state = breaker.getState(modelId);
        state.setOpenTime(System.currentTimeMillis() - (5 * 60 * 1000) - 1);

        assertFalse(breaker.isOpen(modelId));
    }
}
