package com.xbk.knowledge.application.service.selector;

import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证模型选择器在缺省与备用模型场景下的行为，保证选择结果稳定。
 *
 * @author xiexu
 */
public class ModelSelectorTest {

    private ModelConfigRepository modelConfigRepository;
    private TaskTypeRepository taskTypeRepository;
    private ModelSelector modelSelector;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        modelConfigRepository = Mockito.mock(ModelConfigRepository.class);
        taskTypeRepository = Mockito.mock(TaskTypeRepository.class);
        modelSelector = new ModelSelector(modelConfigRepository, taskTypeRepository);
    }

    /**
     * 对外暴露 shouldFallbackToQualityWhenTaskTypeMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldFallbackToQualityWhenTaskTypeMissing() {
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class))).thenReturn(Optional.empty());

        ModelConfig low = ModelConfig.builder()
                .modelName("low")
                .capability(ModelCapability.builder().qualityScore(10).build())
                .build();
        ModelConfig high = ModelConfig.builder()
                .modelName("high")
                .capability(ModelCapability.builder().qualityScore(90).build())
                .build();
        when(modelConfigRepository.findByEnabledTrueWithCapability()).thenReturn(Arrays.asList(low, high));

        ModelSelectionResult result = modelSelector.selectModel("task");

        assertEquals("high", result.getPrimaryModel().getModelName());
        assertTrue(result.getFallbackModels().isEmpty());
    }

    /**
     * 对外暴露 shouldFallbackWhenPreferredModelDisabled 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldFallbackWhenPreferredModelDisabled() {
        TaskType taskType = TaskType.builder()
                .taskCode("task")
                .preferredModelId(1L)
                .build();
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class))).thenReturn(Optional.of(taskType));

        ModelConfig disabled = ModelConfig.builder().id(1L).modelName("disabled").enabled(false).build();
        when(modelConfigRepository.findById(any(IdQuery.class))).thenReturn(Optional.of(disabled));

        ModelConfig available = ModelConfig.builder().modelName("available").build();
        when(modelConfigRepository.findByEnabledTrueWithCapability()).thenReturn(Collections.<ModelConfig>singletonList(available));

        ModelSelectionResult result = modelSelector.selectModel("task");

        assertEquals("available", result.getPrimaryModel().getModelName());
    }

    /**
     * 对外暴露 shouldParseFallbackIds 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldParseFallbackIds() {
        TaskType taskType = TaskType.builder()
                .taskCode("task")
                .preferredModelId(1L)
                .fallbackModelIds("1, 2,2")
                .build();
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class))).thenReturn(Optional.of(taskType));

        ModelConfig primary = ModelConfig.builder().id(1L).modelName("primary").enabled(true).build();
        when(modelConfigRepository.findById(any(IdQuery.class))).thenReturn(Optional.of(primary));
        when(modelConfigRepository.findEnabledByIds(any(EnabledIdsQuery.class))).thenReturn(Collections.<ModelConfig>emptyList());

        modelSelector.selectModel("task");

        ArgumentCaptor<EnabledIdsQuery> captor = ArgumentCaptor.forClass(EnabledIdsQuery.class);
        Mockito.verify(modelConfigRepository).findEnabledByIds(captor.capture());
        List<Long> ids = captor.getValue().getIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(2L));
    }
}
