package com.xbk.knowledge.application.service.selection;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.service.selector.ModelSelector;
import com.xbk.knowledge.application.service.selection.handler.DefaultSelectionHandler;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 验证默认选择处理器的兜底策略，避免无模型可选。
 *
 * @author xiexu
 */
public class DefaultSelectionHandlerTest {

    /**
     * 对外暴露 shouldAlwaysSelectQualityPriorityModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldAlwaysSelectQualityPriorityModel() {
        ModelSelector selector = Mockito.mock(ModelSelector.class);
        ModelConfig modelConfig = ModelConfig.builder().modelName("default").build();
        when(selector.selectByQualityPriority()).thenReturn(modelConfig);

        DefaultSelectionHandler handler = new DefaultSelectionHandler();
        ReflectionTestUtils.setField(handler, "modelSelector", selector);
        AICallCommand command = AICallCommand.builder().content("hi").build();

        assertEquals("default", handler.select(command).getSelectedModel().getModelName());
    }
}
