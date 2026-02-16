package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.aggregate.model.ModelConfigAggregate;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeQuery;
import com.xbk.knowledge.domain.repository.model.ModelConfigRepository;
import com.xbk.knowledge.types.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证模型配置领域服务的校验与聚合更新逻辑，避免配置数据异常。
 *
 * @author xiexu
 */
public class ModelConfigServiceImplTest {

    private ModelConfigRepository modelConfigRepository;
    private ModelConfigServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        modelConfigRepository = Mockito.mock(ModelConfigRepository.class);
        service = new ModelConfigServiceImpl(modelConfigRepository);
    }

    /**
     * 对外暴露 shouldRejectNullPageQuery 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectNullPageQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.queryModelConfigPage(null));
    }

    /**
     * 对外暴露 shouldDefaultPageQueryWhenValuesMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDefaultPageQueryWhenValuesMissing() {
        ModelConfigPageQuery query = new ModelConfigPageQuery(null, null);
        List<ModelConfig> models = Collections.emptyList();
        when(modelConfigRepository.findPageWithCapability(any(ModelConfigPageQuery.class))).thenReturn(models);
        when(modelConfigRepository.countAll()).thenReturn(0L);

        service.queryModelConfigPage(query);

        ArgumentCaptor<ModelConfigPageQuery> captor = ArgumentCaptor.forClass(ModelConfigPageQuery.class);
        verify(modelConfigRepository).findPageWithCapability(captor.capture());
        assertEquals(0, captor.getValue().getOffset());
        assertEquals(10, captor.getValue().getPageSize());
    }

    /**
     * 对外暴露 shouldRejectDuplicateModelNameOnCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectDuplicateModelNameOnCreate() {
        ModelConfig modelConfig = ModelConfig.builder().modelName("gpt").build();
        when(modelConfigRepository.findByModelName(any(ModelNameQuery.class)))
                .thenReturn(Optional.of(ModelConfig.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> service.createModelConfig(modelConfig));
    }

    /**
     * 对外暴露 shouldSetTimestampsOnCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSetTimestampsOnCreate() {
        ModelCapability capability = ModelCapability.builder().qualityScore(80).build();
        ModelConfig modelConfig = ModelConfig.builder()
                .modelName("gpt")
                .capability(capability)
                .build();
        when(modelConfigRepository.findByModelName(any(ModelNameQuery.class)))
                .thenReturn(Optional.empty());
        when(modelConfigRepository.save(any(ModelConfigAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelConfig saved = service.createModelConfig(modelConfig);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getCapability().getCreatedAt());
        assertNotNull(saved.getCapability().getUpdatedAt());
    }

    /**
     * 对外暴露 shouldRejectUpdateWithoutId 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectUpdateWithoutId() {
        ModelConfig modelConfig = ModelConfig.builder().build();
        assertThrows(IllegalArgumentException.class, () -> service.updateModelConfig(modelConfig));
    }

    /**
     * 对外暴露 shouldUpdateFieldsAndCapability 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldUpdateFieldsAndCapability() {
        ModelConfig existing = ModelConfig.builder()
                .id(1L)
                .modelName("old")
                .build();
        when(modelConfigRepository.findById(any(IdQuery.class))).thenReturn(Optional.of(existing));
        when(modelConfigRepository.findByModelName(any(ModelNameQuery.class))).thenReturn(Optional.of(existing));
        when(modelConfigRepository.save(any(ModelConfigAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelCapability capability = ModelCapability.builder()
                .maxInputTokens(10)
                .maxOutputTokens(20)
                .qualityScore(90)
                .build();
        ModelConfig request = ModelConfig.builder()
                .id(1L)
                .modelName("new")
                .apiKey("k")
                .baseUrl("url")
                .enabled(true)
                .priority(2)
                .capability(capability)
                .build();

        ModelConfig updated = service.updateModelConfig(request);

        assertEquals("new", updated.getModelName());
        assertEquals("k", updated.getApiKey());
        assertEquals("url", updated.getBaseUrl());
        assertTrue(updated.getEnabled());
        assertEquals(Integer.valueOf(2), updated.getPriority());
        assertNotNull(updated.getCapability());
        assertEquals(Integer.valueOf(10), updated.getCapability().getMaxInputTokens());
    }

    /**
     * 对外暴露 shouldThrowWhenDeleteMissingModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldThrowWhenDeleteMissingModel() {
        when(modelConfigRepository.existsById(any(IdQuery.class))).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.deleteModelConfig(new IdQuery(1L)));
    }

    /**
     * 对外暴露 shouldReturnFirstEnabledModelAsRecommendation 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnFirstEnabledModelAsRecommendation() {
        ModelConfig first = ModelConfig.builder().modelName("first").build();
        when(modelConfigRepository.findByEnabled(any(EnabledQuery.class))).thenReturn(Collections.<ModelConfig>singletonList(first));

        ModelConfig result = service.getRecommendedModel(new TaskTypeQuery("task"));

        assertEquals("first", result.getModelName());
    }

    /**
     * 对外暴露 shouldReturnNullWhenNoEnabledModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnNullWhenNoEnabledModel() {
        when(modelConfigRepository.findByEnabled(any(EnabledQuery.class))).thenReturn(Collections.<ModelConfig>emptyList());

        ModelConfig result = service.getRecommendedModel(new TaskTypeQuery("task"));

        assertNull(result);
    }
}
