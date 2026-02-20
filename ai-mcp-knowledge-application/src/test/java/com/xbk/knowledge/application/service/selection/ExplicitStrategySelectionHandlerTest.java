package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.service.selector.ModelSelector;
import com.xbk.knowledge.application.service.selection.handler.ExplicitStrategySelectionHandler;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import com.xbk.knowledge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 验证显式策略处理器的优先策略与异常分支，避免策略失效。
 *
 * @author xiexu
 */
public class ExplicitStrategySelectionHandlerTest {

    /**
     * 对外暴露 shouldSelectModelForQualityStrategy 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSelectModelForQualityStrategy() {
        ModelSelector selector = Mockito.mock(ModelSelector.class);
        ModelConfig modelConfig = ModelConfig.builder().modelName("best").build();
        when(selector.selectByQualityPriority()).thenReturn(modelConfig);

        ExplicitStrategySelectionHandler handler = new ExplicitStrategySelectionHandler();
        ReflectionTestUtils.setField(handler, "modelSelector", selector);
        AICallCommand command = AICallCommand.builder()
                .content("hi")
                .strategy(ModelSelectionStrategy.QUALITY_PRIORITY)
                .build();

        assertEquals("best", handler.select(command).getSelectedModel().getModelName());
    }

    /**
     * 对外暴露 shouldThrowForUnsupportedStrategy 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldThrowForUnsupportedStrategy() {
        ModelSelector selector = Mockito.mock(ModelSelector.class);
        ExplicitStrategySelectionHandler handler = new ExplicitStrategySelectionHandler();
        ReflectionTestUtils.setField(handler, "modelSelector", selector);
        AICallCommand command = AICallCommand.builder()
                .content("hi")
                .strategy(ModelSelectionStrategy.SPEED_PRIORITY)
                .build();

        assertThrows(BusinessException.class, () -> handler.select(command));
    }
}
