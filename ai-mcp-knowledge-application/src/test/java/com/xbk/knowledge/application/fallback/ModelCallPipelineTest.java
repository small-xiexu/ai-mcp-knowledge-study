package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
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
                (context, chain) -> {
                    calls.add("p1");
                    return chain.proceed(context);
                },
                (context, chain) -> {
                    calls.add("p2");
                    return chain.proceed(context);
                }
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
}
