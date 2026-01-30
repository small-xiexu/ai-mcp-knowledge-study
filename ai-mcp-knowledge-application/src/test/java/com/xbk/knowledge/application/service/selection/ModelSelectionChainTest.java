package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证模型选择责任链的匹配顺序，避免选择逻辑被跳过。
 *
 * @author xiexu
 */
public class ModelSelectionChainTest {

    /**
     * 对外暴露 shouldReturnDecisionFromFirstSupportingHandler 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnDecisionFromFirstSupportingHandler() {
        ModelConfig modelConfig = ModelConfig.builder().modelName("m1").build();
        ModelSelectionDecision decision = ModelSelectionDecision.byModel(modelConfig);
        ModelSelectionHandler handler1 = new FixedHandler(false, null);
        ModelSelectionHandler handler2 = new FixedHandler(true, decision);
        ModelSelectionChain chain = new ModelSelectionChain(Arrays.asList(handler1, handler2));

        ModelSelectionDecision result = chain.select(AICallCommand.builder().content("hi").build());

        assertEquals("m1", result.getSelectedModel().getModelName());
    }

    /**
     * 对外暴露 shouldThrowWhenNoHandlerSupports 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldThrowWhenNoHandlerSupports() {
        ModelSelectionChain chain = new ModelSelectionChain(Collections.<ModelSelectionHandler>singletonList(new FixedHandler(false, null)));

        assertThrows(IllegalStateException.class, () -> chain.select(AICallCommand.builder().content("hi").build()));
    }

    private static class FixedHandler implements ModelSelectionHandler {
        private final boolean supports;
        private final ModelSelectionDecision decision;

        private FixedHandler(boolean supports, ModelSelectionDecision decision) {
            this.supports = supports;
            this.decision = decision;
        }

        /**
         * 对外暴露 supports 作为调用入口，便于上层复用。
         */
        @Override
        public boolean supports(AICallCommand request) {
            return supports;
        }

        /**
         * 对外暴露 select 作为调用入口，便于上层复用。
         */
        @Override
        public ModelSelectionDecision select(AICallCommand request) {
            return decision;
        }
    }
}
