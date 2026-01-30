package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证重试策略在失败后的重试计数，避免重试次数丢失。
 *
 * @author xiexu
 */
public class RetryPolicyTest {

    /**
     * 对外暴露 shouldRetryOnceBeforeSuccess 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRetryOnceBeforeSuccess() {
        RetryPolicy policy = new RetryPolicy();
        List<ModelCallOutcome> outcomes = new ArrayList<>();
        outcomes.add(ModelCallOutcome.failed(AICallResult.builder().success(false).errorMessage("fail").build()));
        outcomes.add(ModelCallOutcome.success(AICallResult.builder().success(true).build()));

        ModelCallOutcome result = policy.apply(ModelCallContext.builder().build(), new StubChain(outcomes));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getResult().getRetryCount());
    }

    private static class StubChain implements ModelCallPolicyChain {
        private final List<ModelCallOutcome> outcomes;
        private int index;

        private StubChain(List<ModelCallOutcome> outcomes) {
            this.outcomes = outcomes;
            this.index = 0;
        }

        /**
         * 对外暴露 proceed 作为调用入口，便于上层复用。
         */
        @Override
        public ModelCallOutcome proceed(ModelCallContext context) {
            if (index >= outcomes.size()) {
                return outcomes.get(outcomes.size() - 1);
            }
            return outcomes.get(index++);
        }
    }
}
