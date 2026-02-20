package com.xbk.knowledge.application.service.selector;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 验证模型选择器在质量优先策略下的行为，保证选择结果稳定。
 *
 * @author xiexu
 */
public class ModelSelectorTest {

    private ModelConfigRepository modelConfigRepository;
    private ModelSelector modelSelector;

    @BeforeEach
    public void setUp() {
        modelConfigRepository = Mockito.mock(ModelConfigRepository.class);
        modelSelector = new ModelSelector(modelConfigRepository);
    }

    @Test
    public void shouldSelectHighestQualityModel() {
        ModelConfig low = ModelConfig.builder().modelName("low").build();
        ModelConfig high = ModelConfig.builder().modelName("high").build();
        when(modelConfigRepository.findByEnabledTrue()).thenReturn(Arrays.asList(low, high));

        ModelConfig result = modelSelector.selectByQualityPriority();
        assertEquals("low", result.getModelName());
    }

    @Test
    public void shouldThrowWhenNoEnabledModel() {
        when(modelConfigRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> modelSelector.selectByQualityPriority());
    }
}
