package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selection.chain.AbstractModelSelectionHandler;
import com.xbk.knowledge.application.service.selection.chain.ModelSelectionChain;
import com.xbk.knowledge.application.service.selection.handler.ModelSelectionHandler;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;

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
        handler1.appendNext(handler2);
        ModelSelectionChain chain = new ModelSelectionChain(handler1);

        ModelSelectionDecision result = chain.select(AICallCommand.builder().content("hi").build());

        assertEquals("m1", result.getSelectedModel().getModelName());
    }

    /**
     * 对外暴露 shouldThrowWhenNoHandlerSupports 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldThrowWhenNoHandlerSupports() {
        ModelSelectionChain chain = new ModelSelectionChain(new FixedHandler(false, null));

        assertThrows(IllegalStateException.class, () -> chain.select(AICallCommand.builder().content("hi").build()));
    }

    private static class FixedHandler extends AbstractModelSelectionHandler {
        private final boolean supports;
        private final ModelSelectionDecision decision;

        private FixedHandler(boolean supports, ModelSelectionDecision decision) {
            this.supports = supports;
            this.decision = decision;
        }

        
        @Override
        public boolean supports(AICallCommand request) {
            return supports;
        }

        
        @Override
        protected ModelSelectionDecision doSelect(AICallCommand request) {
            if (!supports) {
                if (next() == null) {
                    throw new IllegalStateException("模型选择处理器未配置");
                }
                return next().select(request);
            }
            return decision;
        }
    }
}
