package com.xbk.knowledge.application.fallback.chain;

import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.core.ModelCallOutcome;
import com.xbk.knowledge.application.fallback.executor.ModelCallExecutor;
import com.xbk.knowledge.application.fallback.policy.AbstractModelCallPolicy;
import com.xbk.knowledge.application.fallback.policy.ModelCallPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证模型调用管道的责任链顺序与最终结果，确保策略可叠加。
 *
 * @author xiexu
 */
public class ModelCallPipelineTest {

    /**
     * 对外暴露 shouldExecutePoliciesInOrder 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldExecutePoliciesInOrder() {
        List<String> calls = new ArrayList<>();
        List<ModelCallPolicy> policies = Arrays.asList(
                new RecordingPolicy("p1", calls),
                new RecordingPolicy("p2", calls)
        );
        ModelCallExecutor executor = context -> {
            calls.add("exec");
            return AICallResult.builder().success(true).build();
        };

        ModelCallPipeline pipeline = new ModelCallPipeline(policies, executor);
        ModelCallOutcome outcome = pipeline.execute(ModelCallContext.builder().build());

        assertTrue(outcome.isSuccess());
        assertEquals(Arrays.asList("p1", "p2", "exec"), calls);
    }

    /**
     * 对外暴露 shouldReturnFailedOutcomeWhenExecutorFails 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnFailedOutcomeWhenExecutorFails() {
        ModelCallExecutor executor = context -> AICallResult.builder().success(false).errorMessage("fail").build();
        ModelCallPipeline pipeline = new ModelCallPipeline(Collections.<ModelCallPolicy>emptyList(), executor);

        ModelCallOutcome outcome = pipeline.execute(ModelCallContext.builder().build());

        assertFalse(outcome.isSuccess());
    }

    private static class RecordingPolicy extends AbstractModelCallPolicy {
        private final String name;
        private final List<String> calls;

        private RecordingPolicy(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public ModelCallOutcome apply(ModelCallContext context) {
            calls.add(name);
            return next().apply(context);
        }
    }
}
